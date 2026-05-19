#!/usr/bin/env python3
import ipaddress
import json
import os
import pathlib
import stat
import sys
import time
from dataclasses import dataclass
from typing import List, Optional

import boto3
from botocore.exceptions import ClientError
from dotenv import load_dotenv


RAM_CATALOG_GIB = {
    # burstable family
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
    # general purpose
    "m7i.large": 8,
    "m7i.xlarge": 16,
    "m7i.2xlarge": 32,
    "m7i.4xlarge": 64,
    "m6i.large": 8,
    "m6i.xlarge": 16,
    "m6i.2xlarge": 32,
    "m6i.4xlarge": 64,
}


@dataclass
class Config:
    region: str
    profile: Optional[str]
    instance_type: Optional[str]
    min_ram_gib: Optional[int]
    root_volume_gb: int
    vpc_id: Optional[str]
    subnet_id: Optional[str]
    security_group_name: str
    security_group_id: Optional[str]
    open_ports: List[int]
    key_pair_name: str
    create_key_pair: bool
    key_output_path: str
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
        instance_type=instance_type,
        min_ram_gib=min_ram_gib,
        root_volume_gb=root_volume_gb,
        vpc_id=os.getenv("VPC_ID", "").strip() or None,
        subnet_id=os.getenv("SUBNET_ID", "").strip() or None,
        security_group_name=os.getenv("SECURITY_GROUP_NAME", "be-forum-sg").strip(),
        security_group_id=os.getenv("SECURITY_GROUP_ID", "").strip() or None,
        open_ports=open_ports,
        key_pair_name=os.getenv("KEY_PAIR_NAME", "be-forum-key").strip(),
        create_key_pair=env_bool("CREATE_KEY_PAIR", True),
        key_output_path=os.getenv("KEY_OUTPUT_PATH", "./provisioning/keys").strip(),
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

    # pick smallest RAM first, then lexicographically
    candidates.sort(key=lambda x: (x[1], x[0]))
    return candidates[0][0]


def get_default_vpc_id(ec2_client) -> str:
    res = ec2_client.describe_vpcs(Filters=[{"Name": "isDefault", "Values": ["true"]}])
    vpcs = res.get("Vpcs", [])
    if not vpcs:
        raise RuntimeError("No default VPC found; set VPC_ID explicitly")
    return vpcs[0]["VpcId"]


def get_default_subnet_id(ec2_client, vpc_id: str) -> str:
    # Prefer default-for-az subnet in this VPC
    res = ec2_client.describe_subnets(
        Filters=[
            {"Name": "vpc-id", "Values": [vpc_id]},
            {"Name": "default-for-az", "Values": ["true"]},
        ]
    )
    subnets = res.get("Subnets", [])
    if subnets:
        return subnets[0]["SubnetId"]

    # fallback: any subnet in VPC
    res2 = ec2_client.describe_subnets(Filters=[{"Name": "vpc-id", "Values": [vpc_id]}])
    subnets2 = res2.get("Subnets", [])
    if not subnets2:
        raise RuntimeError(f"No subnet found in VPC {vpc_id}; set SUBNET_ID explicitly")
    return subnets2[0]["SubnetId"]


def ensure_key_pair(ec2_client, cfg: Config) -> None:
    try:
        ec2_client.describe_key_pairs(KeyNames=[cfg.key_pair_name])
        print(f"[ok] Reusing existing key pair: {cfg.key_pair_name}")
        return
    except ClientError as e:
        msg = str(e)
        # Some restricted IAM roles (e.g., AWS Academy) deny DescribeKeyPairs.
        # In that case we proceed and let RunInstances validate KEY_PAIR_NAME.
        if "UnauthorizedOperation" in msg or "AccessDenied" in msg:
            print(
                "[warn] Missing permission for ec2:DescribeKeyPairs. "
                "Skipping precheck; RunInstances will validate KEY_PAIR_NAME."
            )
            if cfg.create_key_pair:
                print(
                    "[warn] CREATE_KEY_PAIR is true but cannot verify/create without "
                    "DescribeKeyPairs permission. Using existing KEY_PAIR_NAME only."
                )
            return

        if "InvalidKeyPair.NotFound" not in msg:
            raise

    if not cfg.create_key_pair:
        raise RuntimeError(
            f"Key pair '{cfg.key_pair_name}' not found and CREATE_KEY_PAIR=false"
        )

    if cfg.dry_run:
        print(f"[dry-run] Would create key pair: {cfg.key_pair_name}")
        return

    outdir = pathlib.Path(cfg.key_output_path)
    outdir.mkdir(parents=True, exist_ok=True)

    created = ec2_client.create_key_pair(KeyName=cfg.key_pair_name)
    key_material = created["KeyMaterial"]

    pem_path = outdir / f"{cfg.key_pair_name}.pem"
    pem_path.write_text(key_material, encoding="utf-8")
    pem_path.chmod(stat.S_IRUSR | stat.S_IWUSR)

    print(f"[ok] Created key pair: {cfg.key_pair_name}")
    print(f"[ok] Saved private key: {pem_path}")


def ensure_security_group(ec2_client, cfg: Config, vpc_id: str) -> str:
    if cfg.security_group_id:
        print(f"[ok] Using provided security group ID: {cfg.security_group_id}")
        return cfg.security_group_id

    existing = ec2_client.describe_security_groups(
        Filters=[
            {"Name": "group-name", "Values": [cfg.security_group_name]},
            {"Name": "vpc-id", "Values": [vpc_id]},
        ]
    ).get("SecurityGroups", [])

    if existing:
        sg_id = existing[0]["GroupId"]
        print(f"[ok] Reusing security group: {cfg.security_group_name} ({sg_id})")
    else:
        if cfg.dry_run:
            print(f"[dry-run] Would create security group: {cfg.security_group_name}")
            sg_id = "sg-dryrun"
        else:
            created = ec2_client.create_security_group(
                GroupName=cfg.security_group_name,
                Description="Security group for be-forum EC2",
                VpcId=vpc_id,
            )
            sg_id = created["GroupId"]
            print(f"[ok] Created security group: {cfg.security_group_name} ({sg_id})")

    if cfg.dry_run:
        print(f"[dry-run] Would authorize ingress ports: {cfg.open_ports} on {sg_id}")
        return sg_id

    current = ec2_client.describe_security_groups(GroupIds=[sg_id])["SecurityGroups"][0]
    existing_rules = current.get("IpPermissions", [])

    for port in cfg.open_ports:
        already = False
        for rule in existing_rules:
            if (
                rule.get("IpProtocol") == "tcp"
                and rule.get("FromPort") == port
                and rule.get("ToPort") == port
            ):
                for r in rule.get("IpRanges", []):
                    if r.get("CidrIp") == "0.0.0.0/0":
                        already = True
                        break
            if already:
                break

        if already:
            continue

        try:
            ec2_client.authorize_security_group_ingress(
                GroupId=sg_id,
                IpPermissions=[
                    {
                        "IpProtocol": "tcp",
                        "FromPort": port,
                        "ToPort": port,
                        "IpRanges": [{"CidrIp": "0.0.0.0/0", "Description": f"open tcp/{port}"}],
                    }
                ],
            )
            print(f"[ok] Opened tcp/{port} to 0.0.0.0/0")
        except ClientError as e:
            # Duplicate rule race / already exists
            if "InvalidPermission.Duplicate" in str(e):
                pass
            else:
                raise

    return sg_id


def ubuntu_2404_ami_id(ec2_client, ssm_client, cfg: Config) -> str:
    if cfg.ami_id:
        return cfg.ami_id

    # Primary method: Canonical SSM public parameter for Ubuntu 24.04 LTS amd64.
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

    # Fallback for restricted roles: resolve latest Canonical Ubuntu 24.04 AMI via EC2.
    images = ec2_client.describe_images(
        Owners=["099720109477"],  # Canonical
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


def user_data_script() -> str:
    return """#!/bin/bash
set -euxo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release git

install -m 0755 -d /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
fi
chmod a+r /etc/apt/keyrings/docker.gpg

ARCH=$(dpkg --print-architecture)
CODENAME=$( . /etc/os-release && echo "$VERSION_CODENAME" )
echo \
  "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker

if id ubuntu >/dev/null 2>&1; then
  usermod -aG docker ubuntu
fi
"""


def run_instance(ec2_resource, cfg: Config, ami_id: str, instance_type: str, subnet_id: str, sg_id: str):
    tags = [
        {"Key": "Name", "Value": cfg.instance_name},
        {"Key": "Project", "Value": cfg.project_tag},
        {"Key": "Environment", "Value": cfg.env_tag},
    ]

    launch_args = {
        "ImageId": ami_id,
        "InstanceType": instance_type,
        "MinCount": 1,
        "MaxCount": 1,
        "KeyName": cfg.key_pair_name,
        "SubnetId": subnet_id,
        "SecurityGroupIds": [sg_id],
        "TagSpecifications": [
            {"ResourceType": "instance", "Tags": tags},
            {"ResourceType": "volume", "Tags": tags},
        ],
        "BlockDeviceMappings": [
            {
                "DeviceName": "/dev/sda1",
                "Ebs": {
                    "VolumeSize": cfg.root_volume_gb,
                    "VolumeType": "gp3",
                    "DeleteOnTermination": True,
                    "Encrypted": True,
                },
            }
        ],
        "UserData": user_data_script(),
    }

    if cfg.dry_run:
        print("[dry-run] Would launch instance with config:")
        print(json.dumps({
            "ImageId": ami_id,
            "InstanceType": instance_type,
            "SubnetId": subnet_id,
            "SecurityGroupId": sg_id,
            "RootVolumeGiB": cfg.root_volume_gb,
            "Tags": tags,
        }, indent=2))
        return None

    instances = ec2_resource.create_instances(**launch_args)
    instance = instances[0]
    print(f"[ok] Launched instance: {instance.id}")
    print("[wait] Waiting for instance to be running...")
    instance.wait_until_running()
    instance.reload()
    print(f"[ok] Instance running: {instance.id}")
    print(f"[ok] Public IP: {instance.public_ip_address}")
    return instance


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


def associate_eip(ec2_client, instance_id: str, allocation_id: str, dry_run: bool) -> None:
    if dry_run:
        print(f"[dry-run] Would associate EIP {allocation_id} -> instance {instance_id}")
        return

    # If EIP already associated to this instance, this call is idempotent with AllowReassociation.
    ec2_client.associate_address(
        InstanceId=instance_id,
        AllocationId=allocation_id,
        AllowReassociation=True,
    )
    print(f"[ok] Associated EIP {allocation_id} with instance {instance_id}")


def validate_aws_region_name(region: str) -> None:
    if not region or len(region.split("-")) < 3:
        raise ValueError(f"Invalid AWS region format: {region}")


def main() -> int:
    try:
        cfg = load_config()
        validate_aws_region_name(cfg.region)

        session = make_session(cfg)
        ec2_client = session.client("ec2")
        ec2_resource = session.resource("ec2")
        ssm_client = session.client("ssm")

        instance_type = resolve_instance_type(cfg)
        print(f"[cfg] Region: {cfg.region}")
        print(f"[cfg] Instance type: {instance_type}")
        print(f"[cfg] Root volume: {cfg.root_volume_gb} GiB")
        print(f"[cfg] Open ports: {cfg.open_ports}")

        vpc_id = cfg.vpc_id or get_default_vpc_id(ec2_client)
        subnet_id = cfg.subnet_id or get_default_subnet_id(ec2_client, vpc_id)
        print(f"[cfg] VPC: {vpc_id}")
        print(f"[cfg] Subnet: {subnet_id}")

        ensure_key_pair(ec2_client, cfg)
        sg_id = ensure_security_group(ec2_client, cfg, vpc_id)

        ami_id = ubuntu_2404_ami_id(ec2_client, ssm_client, cfg)
        print(f"[cfg] Ubuntu 24.04 AMI: {ami_id}")

        instance = run_instance(ec2_resource, cfg, ami_id, instance_type, subnet_id, sg_id)

        alloc_id = resolve_allocation_id(ec2_client, cfg)
        if alloc_id:
            target_instance_id = instance.id if instance is not None else "i-dryrun"
            associate_eip(ec2_client, target_instance_id, alloc_id, cfg.dry_run)

        if instance is not None:
            instance.reload()
            print("\n=== Provisioning Complete ===")
            print(f"Instance ID: {instance.id}")
            print(f"Instance Type: {instance.instance_type}")
            print(f"Public IP: {instance.public_ip_address}")
            print(f"Private IP: {instance.private_ip_address}")
            print(f"AZ: {instance.placement['AvailabilityZone']}")
            print(f"Security Group: {sg_id}")
            print(f"Key Pair: {cfg.key_pair_name}")

        return 0
    except Exception as exc:
        print(f"[error] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
