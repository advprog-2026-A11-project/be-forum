#!/usr/bin/env python3
import os
import sys
import time
from dataclasses import dataclass
from typing import List, Optional

import boto3
from botocore.exceptions import ClientError
from dotenv import load_dotenv


def env_bool(key: str, default: bool = False) -> bool:
    raw = os.getenv(key)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "y", "on"}


@dataclass
class DestroyConfig:
    region: str
    profile: Optional[str]
    instance_id: Optional[str]
    instance_name: str
    project_tag: str
    env_tag: str
    eip_allocation_id: Optional[str]
    eip_public_ip: Optional[str]
    release_eip: bool
    security_group_id: Optional[str]
    security_group_name: str
    vpc_id: Optional[str]
    key_pair_name: Optional[str]
    delete_key_pair: bool
    delete_local_key_file: bool
    key_output_path: str
    force: bool
    dry_run: bool


def load_config() -> DestroyConfig:
    load_dotenv()

    region = os.getenv("AWS_REGION", "").strip()
    if not region:
        raise ValueError("AWS_REGION is required")

    return DestroyConfig(
        region=region,
        profile=os.getenv("AWS_PROFILE", "").strip() or None,
        instance_id=os.getenv("DESTROY_INSTANCE_ID", "").strip() or None,
        instance_name=os.getenv("INSTANCE_NAME", "be-forum-ec2").strip(),
        project_tag=os.getenv("PROJECT_TAG", "be-forum").strip(),
        env_tag=os.getenv("ENV_TAG", "dev").strip(),
        eip_allocation_id=os.getenv("EIP_ALLOCATION_ID", "").strip() or None,
        eip_public_ip=os.getenv("EIP_PUBLIC_IP", "").strip() or None,
        release_eip=env_bool("RELEASE_EIP_ON_DESTROY", False),
        security_group_id=os.getenv("SECURITY_GROUP_ID", "").strip() or None,
        security_group_name=os.getenv("SECURITY_GROUP_NAME", "be-forum-sg").strip(),
        vpc_id=os.getenv("VPC_ID", "").strip() or None,
        key_pair_name=os.getenv("KEY_PAIR_NAME", "").strip() or None,
        delete_key_pair=env_bool("DELETE_KEY_PAIR_ON_DESTROY", False),
        delete_local_key_file=env_bool("DELETE_LOCAL_KEY_FILE_ON_DESTROY", False),
        key_output_path=os.getenv("KEY_OUTPUT_PATH", "./provisioning/keys").strip(),
        force=env_bool("DESTROY_FORCE", False),
        dry_run=env_bool("DRY_RUN", False),
    )


def make_session(cfg: DestroyConfig):
    if cfg.profile:
        return boto3.Session(profile_name=cfg.profile, region_name=cfg.region)
    return boto3.Session(region_name=cfg.region)


def resolve_instance_id(ec2_client, cfg: DestroyConfig) -> str:
    if cfg.instance_id:
        return cfg.instance_id

    filters = [
        {"Name": "tag:Name", "Values": [cfg.instance_name]},
        {"Name": "instance-state-name", "Values": ["pending", "running", "stopping", "stopped"]},
    ]

    # Extra narrowing to avoid accidental deletion if names collide.
    if cfg.project_tag:
        filters.append({"Name": "tag:Project", "Values": [cfg.project_tag]})
    if cfg.env_tag:
        filters.append({"Name": "tag:Environment", "Values": [cfg.env_tag]})

    res = ec2_client.describe_instances(Filters=filters)
    candidates: List[str] = []
    for reservation in res.get("Reservations", []):
        for inst in reservation.get("Instances", []):
            candidates.append(inst["InstanceId"])

    if not candidates:
        raise RuntimeError(
            "No instance found by tags. Set DESTROY_INSTANCE_ID explicitly to destroy a specific instance."
        )

    if len(candidates) > 1:
        raise RuntimeError(
            f"Multiple instances matched {candidates}. Set DESTROY_INSTANCE_ID explicitly."
        )

    return candidates[0]


def maybe_confirm(cfg: DestroyConfig, instance_id: str) -> None:
    if cfg.force:
        return

    print("\n[confirm] About to destroy instance:")
    print(f"  Region: {cfg.region}")
    print(f"  Instance ID: {instance_id}")
    print(f"  Dry run: {cfg.dry_run}")
    print("Set DESTROY_FORCE=true to skip confirmation.")
    answer = input("Type 'DESTROY' to continue: ").strip()
    if answer != "DESTROY":
        raise RuntimeError("Destroy aborted by user")


def allocation_id_from_ip(ec2_client, public_ip: str) -> str:
    res = ec2_client.describe_addresses(PublicIps=[public_ip])
    addrs = res.get("Addresses", [])
    if not addrs:
        raise RuntimeError(f"No Elastic IP found for {public_ip}")
    alloc = addrs[0].get("AllocationId")
    if not alloc:
        raise RuntimeError(f"Elastic IP {public_ip} has no allocation id")
    return alloc


def detach_eip_if_attached(ec2_client, instance_id: str, allocation_id: Optional[str], dry_run: bool) -> None:
    if not allocation_id:
        return

    addresses = ec2_client.describe_addresses(AllocationIds=[allocation_id]).get("Addresses", [])
    if not addresses:
        print(f"[warn] EIP allocation {allocation_id} not found")
        return

    addr = addresses[0]
    assoc_id = addr.get("AssociationId")
    attached_instance = addr.get("InstanceId")

    if not assoc_id:
        print(f"[ok] EIP {allocation_id} is not associated")
        return

    if attached_instance and attached_instance != instance_id:
        print(
            f"[warn] EIP {allocation_id} is attached to another instance {attached_instance}; not disassociating"
        )
        return

    if dry_run:
        print(f"[dry-run] Would disassociate EIP {allocation_id} (assoc {assoc_id})")
        return

    ec2_client.disassociate_address(AssociationId=assoc_id)
    print(f"[ok] Disassociated EIP {allocation_id}")


def release_eip(ec2_client, allocation_id: Optional[str], enabled: bool, dry_run: bool) -> None:
    if not enabled or not allocation_id:
        return
    if dry_run:
        print(f"[dry-run] Would release EIP allocation {allocation_id}")
        return

    try:
        ec2_client.release_address(AllocationId=allocation_id)
        print(f"[ok] Released EIP allocation {allocation_id}")
    except ClientError as exc:
        print(f"[warn] Failed to release EIP {allocation_id}: {exc}")


def terminate_instance(ec2_client, instance_id: str, dry_run: bool) -> None:
    if dry_run:
        print(f"[dry-run] Would terminate instance {instance_id}")
        return

    ec2_client.terminate_instances(InstanceIds=[instance_id])
    print(f"[ok] Terminate requested for {instance_id}")

    waiter = ec2_client.get_waiter("instance_terminated")
    print("[wait] Waiting for instance termination...")
    waiter.wait(InstanceIds=[instance_id])
    print(f"[ok] Instance terminated: {instance_id}")


def resolve_security_group_id(ec2_client, cfg: DestroyConfig) -> Optional[str]:
    if cfg.security_group_id:
        return cfg.security_group_id

    filters = [{"Name": "group-name", "Values": [cfg.security_group_name]}]
    if cfg.vpc_id:
        filters.append({"Name": "vpc-id", "Values": [cfg.vpc_id]})

    sgs = ec2_client.describe_security_groups(Filters=filters).get("SecurityGroups", [])
    if not sgs:
        return None
    if len(sgs) > 1:
        print("[warn] Multiple security groups matched; set SECURITY_GROUP_ID explicitly if you want cleanup")
        return None
    return sgs[0]["GroupId"]


def delete_security_group_if_safe(ec2_client, sg_id: Optional[str], dry_run: bool) -> None:
    if not sg_id:
        return

    ni = ec2_client.describe_network_interfaces(
        Filters=[{"Name": "group-id", "Values": [sg_id]}]
    ).get("NetworkInterfaces", [])
    if ni:
        print(f"[warn] Security group {sg_id} still attached to network interfaces; skipping delete")
        return

    if dry_run:
        print(f"[dry-run] Would delete security group {sg_id}")
        return

    try:
        ec2_client.delete_security_group(GroupId=sg_id)
        print(f"[ok] Deleted security group {sg_id}")
    except ClientError as exc:
        print(f"[warn] Failed to delete security group {sg_id}: {exc}")


def delete_keypair_if_requested(ec2_client, cfg: DestroyConfig) -> None:
    if not cfg.delete_key_pair or not cfg.key_pair_name:
        return

    if cfg.dry_run:
        print(f"[dry-run] Would delete key pair {cfg.key_pair_name}")
        return

    try:
        ec2_client.delete_key_pair(KeyName=cfg.key_pair_name)
        print(f"[ok] Deleted key pair {cfg.key_pair_name}")
    except ClientError as exc:
        print(f"[warn] Failed to delete key pair {cfg.key_pair_name}: {exc}")


def delete_local_key_file_if_requested(cfg: DestroyConfig) -> None:
    if not cfg.delete_local_key_file or not cfg.key_pair_name:
        return

    pem = os.path.join(cfg.key_output_path, f"{cfg.key_pair_name}.pem")
    if not os.path.exists(pem):
        print(f"[ok] Local key file not found: {pem}")
        return

    if cfg.dry_run:
        print(f"[dry-run] Would delete local key file {pem}")
        return

    os.remove(pem)
    print(f"[ok] Deleted local key file {pem}")


def main() -> int:
    try:
        cfg = load_config()
        session = make_session(cfg)
        ec2_client = session.client("ec2")

        instance_id = resolve_instance_id(ec2_client, cfg)
        maybe_confirm(cfg, instance_id)

        allocation_id = cfg.eip_allocation_id
        if not allocation_id and cfg.eip_public_ip:
            allocation_id = allocation_id_from_ip(ec2_client, cfg.eip_public_ip)

        # Disassociate EIP first to prevent reassociation issues.
        detach_eip_if_attached(ec2_client, instance_id, allocation_id, cfg.dry_run)

        terminate_instance(ec2_client, instance_id, cfg.dry_run)

        # Optional resource cleanup.
        release_eip(ec2_client, allocation_id, cfg.release_eip, cfg.dry_run)

        sg_id = resolve_security_group_id(ec2_client, cfg)
        delete_security_group_if_safe(ec2_client, sg_id, cfg.dry_run)

        delete_keypair_if_requested(ec2_client, cfg)
        delete_local_key_file_if_requested(cfg)

        print("\n=== Destroy Complete ===")
        print(f"Instance: {instance_id}")
        if allocation_id:
            print(f"EIP allocation: {allocation_id}")
        return 0
    except Exception as exc:
        print(f"[error] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
