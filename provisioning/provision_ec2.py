#!/usr/bin/env python3
import os
import pathlib
import sys
import time
from dataclasses import dataclass
from typing import List, Optional

import boto3
from botocore.exceptions import ClientError
from dotenv import load_dotenv


RAM_CATALOG_GIB = {
    "t3.micro": 1,
    "t3.small": 2,
    "t3.medium": 4,
    "t3.large": 8,
    "t3.xlarge": 16,
    "t3.2xlarge": 32,
    "t4g.micro": 1,
    "t4g.small": 2,
    "t4g.medium": 4,
    "t4g.large": 8,
    "t4g.xlarge": 16,
    "t4g.2xlarge": 32,
    "m6i.large": 8,
    "m6i.xlarge": 16,
    "m6i.2xlarge": 32,
    "m6i.4xlarge": 64,
    "m7i.large": 8,
    "m7i.xlarge": 16,
    "m7i.2xlarge": 32,
    "m7i.4xlarge": 64,
}


@dataclass
class Config:
    region: str
    profile: Optional[str]
    stack_name: str
    instance_type: Optional[str]
    min_ram_gib: Optional[int]
    root_volume_gb: int
    vpc_id: Optional[str]
    subnet_id: Optional[str]
    security_group_name: str
    security_group_id: Optional[str]
    open_ports: List[int]
    key_pair_name: str
    instance_name: str
    project_tag: str
    env_tag: str
    eip_allocation_id: Optional[str]
    eip_public_ip: Optional[str]
    ami_id: Optional[str]
    dry_run: bool


def env_bool(key: str, default: bool = False) -> bool:
    raw = os.getenv(key)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "y", "on"}


def parse_ports(raw: str) -> List[int]:
    ports = []
    for part in raw.split(","):
        part = part.strip()
        if not part:
            continue
        p = int(part)
        if p < 1 or p > 65535:
            raise ValueError(f"Invalid port: {p}")
        ports.append(p)
    if not ports:
        raise ValueError("OPEN_PORTS must contain at least one port")
    return sorted(set(ports))


def load_config() -> Config:
    load_dotenv()

    region = os.getenv("AWS_REGION", "").strip()
    if not region:
        raise ValueError("AWS_REGION is required")

    profile = os.getenv("AWS_PROFILE", "").strip() or None
    instance_type = os.getenv("INSTANCE_TYPE", "").strip() or None
    min_ram_raw = os.getenv("MIN_RAM_GIB", "").strip()
    min_ram_gib = int(min_ram_raw) if min_ram_raw else None

    if not instance_type and not min_ram_gib:
        raise ValueError("Set either INSTANCE_TYPE or MIN_RAM_GIB")

    root_volume_gb = int(os.getenv("ROOT_VOLUME_GB", "30"))
    if root_volume_gb < 8:
        raise ValueError("ROOT_VOLUME_GB must be >= 8")

    open_ports = parse_ports(os.getenv("OPEN_PORTS", "22"))

    return Config(
        region=region,
        profile=profile,
        stack_name=os.getenv("CF_STACK_NAME", "be-forum-ec2").strip(),
        instance_type=instance_type,
        min_ram_gib=min_ram_gib,
        root_volume_gb=root_volume_gb,
        vpc_id=os.getenv("VPC_ID", "").strip() or None,
        subnet_id=os.getenv("SUBNET_ID", "").strip() or None,
        security_group_name=os.getenv("SECURITY_GROUP_NAME", "be-forum-sg").strip(),
        security_group_id=os.getenv("SECURITY_GROUP_ID", "").strip() or None,
        open_ports=open_ports,
        key_pair_name=os.getenv("KEY_PAIR_NAME", "").strip(),
        instance_name=os.getenv("INSTANCE_NAME", "be-forum-ec2").strip(),
        project_tag=os.getenv("PROJECT_TAG", "be-forum").strip(),
        env_tag=os.getenv("ENV_TAG", "dev").strip(),
        eip_allocation_id=os.getenv("EIP_ALLOCATION_ID", "").strip() or None,
        eip_public_ip=os.getenv("EIP_PUBLIC_IP", "").strip() or None,
        ami_id=os.getenv("AMI_ID", "").strip() or None,
        dry_run=env_bool("DRY_RUN", False),
    )


def make_session(cfg: Config):
    if cfg.profile:
        return boto3.Session(profile_name=cfg.profile, region_name=cfg.region)
    return boto3.Session(region_name=cfg.region)


def resolve_instance_type(cfg: Config) -> str:
    if cfg.instance_type:
        return cfg.instance_type

    assert cfg.min_ram_gib is not None
    candidates = [
        (itype, ram) for itype, ram in RAM_CATALOG_GIB.items() if ram >= cfg.min_ram_gib
    ]
    if not candidates:
        raise ValueError(
            f"No instance type found in catalog for MIN_RAM_GIB={cfg.min_ram_gib}. "
            "Set INSTANCE_TYPE directly."
        )

    candidates.sort(key=lambda x: (x[1], x[0]))
    return candidates[0][0]


def get_default_vpc_id(ec2_client) -> str:
    res = ec2_client.describe_vpcs(Filters=[{"Name": "isDefault", "Values": ["true"]}])
    vpcs = res.get("Vpcs", [])
    if not vpcs:
        raise RuntimeError("No default VPC found; set VPC_ID explicitly")
    return vpcs[0]["VpcId"]


def get_default_subnet_id(ec2_client, vpc_id: str) -> str:
    res = ec2_client.describe_subnets(
        Filters=[
            {"Name": "vpc-id", "Values": [vpc_id]},
            {"Name": "default-for-az", "Values": ["true"]},
        ]
    )
    subnets = res.get("Subnets", [])
    if subnets:
        return subnets[0]["SubnetId"]

    res2 = ec2_client.describe_subnets(Filters=[{"Name": "vpc-id", "Values": [vpc_id]}])
    subnets2 = res2.get("Subnets", [])
    if not subnets2:
        raise RuntimeError(f"No subnet found in VPC {vpc_id}; set SUBNET_ID explicitly")
    return subnets2[0]["SubnetId"]


def resolve_allocation_id(ec2_client, cfg: Config) -> Optional[str]:
    if cfg.eip_allocation_id:
        return cfg.eip_allocation_id
    if cfg.eip_public_ip:
        res = ec2_client.describe_addresses(PublicIps=[cfg.eip_public_ip])
        addrs = res.get("Addresses", [])
        if not addrs:
            raise RuntimeError(f"Elastic IP not found for EIP_PUBLIC_IP={cfg.eip_public_ip}")
        return addrs[0].get("AllocationId")
    return None


def ubuntu_2404_ami_id(ec2_client, ssm_client, cfg: Config) -> str:
    if cfg.ami_id:
        return cfg.ami_id

    parameter_name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id"
    try:
        res = ssm_client.get_parameter(Name=parameter_name)
        return res["Parameter"]["Value"]
    except ClientError as exc:
        msg = str(exc)
        if "AccessDenied" not in msg and "UnauthorizedOperation" not in msg:
            raise
        print(
            "[warn] Missing permission for ssm:GetParameter. "
            "Falling back to ec2:DescribeImages for Ubuntu 24.04 AMI discovery."
        )

    images = ec2_client.describe_images(
        Owners=["099720109477"],
        Filters=[
            {"Name": "name", "Values": ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]},
            {"Name": "state", "Values": ["available"]},
            {"Name": "architecture", "Values": ["x86_64"]},
            {"Name": "root-device-type", "Values": ["ebs"]},
            {"Name": "virtualization-type", "Values": ["hvm"]},
        ],
    ).get("Images", [])

    if not images:
        raise RuntimeError(
            "Could not resolve Ubuntu 24.04 AMI via EC2 DescribeImages. "
            "Set AMI_ID explicitly in .env."
        )

    images.sort(key=lambda img: img.get("CreationDate", ""), reverse=True)
    return images[0]["ImageId"]


def validate_aws_region_name(region: str) -> None:
    if not region or len(region.split("-")) < 3:
        raise ValueError(f"Invalid AWS region format: {region}")


def template_body() -> str:
    path = pathlib.Path(__file__).resolve().parent / "ec2-stack.yaml"
    return path.read_text(encoding="utf-8")


def stack_exists(cfn_client, stack_name: str) -> bool:
    try:
        cfn_client.describe_stacks(StackName=stack_name)
        return True
    except ClientError as exc:
        if "does not exist" in str(exc):
            return False
        raise


def to_parameters(cfg: Config, ami_id: str, instance_type: str, vpc_id: str, subnet_id: str, eip_alloc_id: Optional[str]):
    ports = cfg.open_ports[:10]
    while len(ports) < 10:
        ports.append(-1)

    params = {
        "InstanceName": cfg.instance_name,
        "ProjectTag": cfg.project_tag,
        "EnvTag": cfg.env_tag,
        "AmiId": ami_id,
        "InstanceType": instance_type,
        "RootVolumeGiB": str(cfg.root_volume_gb),
        "VpcId": vpc_id,
        "SubnetId": subnet_id,
        "KeyPairName": cfg.key_pair_name,
        "SecurityGroupName": cfg.security_group_name,
        "ExistingSecurityGroupId": cfg.security_group_id or "",
        "Port1": str(ports[0]),
        "Port2": str(ports[1]),
        "Port3": str(ports[2]),
        "Port4": str(ports[3]),
        "Port5": str(ports[4]),
        "Port6": str(ports[5]),
        "Port7": str(ports[6]),
        "Port8": str(ports[7]),
        "Port9": str(ports[8]),
        "Port10": str(ports[9]),
        "EipAllocationId": eip_alloc_id or "",
    }
    return [{"ParameterKey": k, "ParameterValue": v} for k, v in params.items()]


def wait_for_completion(cfn_client, stack_name: str, operation: str) -> None:
    if operation == "create":
        waiter = cfn_client.get_waiter("stack_create_complete")
    else:
        waiter = cfn_client.get_waiter("stack_update_complete")
    waiter.wait(StackName=stack_name)


def print_outputs(cfn_client, stack_name: str) -> None:
    stacks = cfn_client.describe_stacks(StackName=stack_name).get("Stacks", [])
    if not stacks:
        return
    outputs = {o["OutputKey"]: o.get("OutputValue", "") for o in stacks[0].get("Outputs", [])}
    print("\n=== Provisioning Complete (CloudFormation) ===")
    for key in ["StackName", "InstanceId", "PublicIp", "PrivateIp", "AvailabilityZone", "SecurityGroupId"]:
        if key in outputs:
            print(f"{key}: {outputs[key]}")


def main() -> int:
    try:
        cfg = load_config()
        validate_aws_region_name(cfg.region)

        if not cfg.key_pair_name:
            raise ValueError("KEY_PAIR_NAME is required")

        session = make_session(cfg)
        ec2_client = session.client("ec2")
        ssm_client = session.client("ssm")
        cfn_client = session.client("cloudformation")

        instance_type = resolve_instance_type(cfg)
        vpc_id = cfg.vpc_id or get_default_vpc_id(ec2_client)
        subnet_id = cfg.subnet_id or get_default_subnet_id(ec2_client, vpc_id)
        ami_id = ubuntu_2404_ami_id(ec2_client, ssm_client, cfg)
        eip_alloc_id = resolve_allocation_id(ec2_client, cfg)

        print(f"[cfg] Region: {cfg.region}")
        print(f"[cfg] Stack name: {cfg.stack_name}")
        print(f"[cfg] Instance type: {instance_type}")
        print(f"[cfg] Root volume: {cfg.root_volume_gb} GiB")
        print(f"[cfg] VPC: {vpc_id}")
        print(f"[cfg] Subnet: {subnet_id}")
        print(f"[cfg] Ubuntu 24.04 AMI: {ami_id}")
        print(f"[cfg] Open ports: {cfg.open_ports}")
        if eip_alloc_id:
            print(f"[cfg] EIP allocation: {eip_alloc_id}")

        params = to_parameters(cfg, ami_id, instance_type, vpc_id, subnet_id, eip_alloc_id)
        body = template_body()

        exists = stack_exists(cfn_client, cfg.stack_name)
        op = "update" if exists else "create"

        if cfg.dry_run:
            print(f"[dry-run] Would {op} CloudFormation stack {cfg.stack_name}")
            for p in params:
                print(f"  - {p['ParameterKey']}={p['ParameterValue']}")
            return 0

        if op == "create":
            cfn_client.create_stack(
                StackName=cfg.stack_name,
                TemplateBody=body,
                Parameters=params,
                Capabilities=["CAPABILITY_NAMED_IAM"],
                Tags=[
                    {"Key": "Project", "Value": cfg.project_tag},
                    {"Key": "Environment", "Value": cfg.env_tag},
                ],
            )
            print(f"[ok] Create initiated for stack {cfg.stack_name}")
        else:
            try:
                cfn_client.update_stack(
                    StackName=cfg.stack_name,
                    TemplateBody=body,
                    Parameters=params,
                    Capabilities=["CAPABILITY_NAMED_IAM"],
                    Tags=[
                        {"Key": "Project", "Value": cfg.project_tag},
                        {"Key": "Environment", "Value": cfg.env_tag},
                    ],
                )
                print(f"[ok] Update initiated for stack {cfg.stack_name}")
            except ClientError as exc:
                if "No updates are to be performed" in str(exc):
                    print("[ok] No changes detected in stack")
                    print_outputs(cfn_client, cfg.stack_name)
                    return 0
                raise

        print(f"[wait] Waiting for stack {op} to complete...")
        wait_for_completion(cfn_client, cfg.stack_name, op)
        print_outputs(cfn_client, cfg.stack_name)
        return 0
    except Exception as exc:
        print(f"[error] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
