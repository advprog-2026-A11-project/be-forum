# EC2 Provisioning with AWS CloudFormation

This folder provisions and destroys a Ubuntu 24.04 EC2 instance through **CloudFormation** (IaC), driven by environment variables.

What gets configured:
- Region
- VM sizing (instance type or minimum RAM mapping)
- Root disk size
- VPC/subnet
- Security group (existing SG or CF-created SG)
- Optional Elastic IP association
- Bootstrap install of Docker Engine, Docker Compose plugin, and Git

## Prerequisites

- Python 3.10+
- AWS credentials with permissions for CloudFormation + EC2 + (optional) SSM

Install deps from `be-forum/`:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r provisioning/requirements.txt
```

## Configure

```bash
cp provisioning/.env.example provisioning/.env
```

Key variables:
- `AWS_REGION`
- `CF_STACK_NAME`
- `KEY_PAIR_NAME`
- `INSTANCE_TYPE` or `MIN_RAM_GIB`
- `ROOT_VOLUME_GB`
- `VPC_ID`, `SUBNET_ID` (or leave empty to use default)
- `SECURITY_GROUP_ID` (optional; if empty CF creates SG)
- `OPEN_PORTS` (comma-separated TCP ports, up to 10)
- `EIP_ALLOCATION_ID` or `EIP_PUBLIC_IP` (optional)

## Provision / update stack

```bash
python provisioning/provision_ec2.py
```

Dry run:

```bash
DRY_RUN=true python provisioning/provision_ec2.py
```

## Destroy stack

```bash
python provisioning/destroy_ec2.py
```

This deletes all resources in the stack (instance, CF-created SG, and EIP association resource).

Use non-interactive mode:

```bash
DESTROY_FORCE=true python provisioning/destroy_ec2.py
```

## Notes

- If your IAM role cannot call `ssm:GetParameter` / `ec2:DescribeImages`, set `AMI_ID` explicitly.
- `KEY_PAIR_NAME` must exist in the same region.
- CloudFormation template: `provisioning/ec2-stack.yaml`.
