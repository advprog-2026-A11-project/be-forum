#!/usr/bin/env python3
import os
import sys
from dataclasses import dataclass
from typing import Optional

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
    stack_name: str
    force: bool
    dry_run: bool


def load_config() -> DestroyConfig:
    load_dotenv()

    region = os.getenv("AWS_REGION", "").strip()
    if not region:
        raise ValueError("AWS_REGION is required")

    stack_name = os.getenv("CF_STACK_NAME", "be-forum-ec2").strip()
    if not stack_name:
        raise ValueError("CF_STACK_NAME is required")

    return DestroyConfig(
        region=region,
        profile=os.getenv("AWS_PROFILE", "").strip() or None,
        stack_name=stack_name,
        force=env_bool("DESTROY_FORCE", False),
        dry_run=env_bool("DRY_RUN", False),
    )


def make_session(cfg: DestroyConfig):
    if cfg.profile:
        return boto3.Session(profile_name=cfg.profile, region_name=cfg.region)
    return boto3.Session(region_name=cfg.region)


def stack_exists(cfn_client, stack_name: str) -> bool:
    try:
        cfn_client.describe_stacks(StackName=stack_name)
        return True
    except ClientError as exc:
        if "does not exist" in str(exc):
            return False
        raise


def maybe_confirm(cfg: DestroyConfig) -> None:
    if cfg.force:
        return

    print("\n[confirm] About to delete CloudFormation stack:")
    print(f"  Region: {cfg.region}")
    print(f"  Stack: {cfg.stack_name}")
    print(f"  Dry run: {cfg.dry_run}")
    print("Set DESTROY_FORCE=true to skip confirmation.")
    answer = input("Type 'DESTROY' to continue: ").strip()
    if answer != "DESTROY":
        raise RuntimeError("Destroy aborted by user")


def wait_for_delete(cfn_client, stack_name: str) -> None:
    waiter = cfn_client.get_waiter("stack_delete_complete")
    waiter.wait(StackName=stack_name)


def main() -> int:
    try:
        cfg = load_config()
        session = make_session(cfg)
        cfn_client = session.client("cloudformation")

        if not stack_exists(cfn_client, cfg.stack_name):
            print(f"[ok] Stack does not exist: {cfg.stack_name}")
            return 0

        maybe_confirm(cfg)

        if cfg.dry_run:
            print(f"[dry-run] Would delete CloudFormation stack {cfg.stack_name}")
            return 0

        cfn_client.delete_stack(StackName=cfg.stack_name)
        print(f"[ok] Delete initiated for stack {cfg.stack_name}")
        print("[wait] Waiting for stack deletion to complete...")
        wait_for_delete(cfn_client, cfg.stack_name)
        print("\n=== Destroy Complete (CloudFormation) ===")
        print(f"Stack: {cfg.stack_name}")
        return 0
    except Exception as exc:
        print(f"[error] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
