# EC2 Provisioning (AWS Academy Friendly)

This folder provisions and destroys an **Ubuntu Server 24.04 LTS EC2 instance** using Python + boto3, fully driven by environment variables.

It can configure:
- Region
- VM sizing (instance type or minimum RAM)
- Root disk size
- Firewall rules (open TCP ports)
- Existing Elastic IP association
- Bootstrapping packages: **Docker Engine**, **Docker Compose plugin**, and **Git**

It also includes safe teardown support.

## 1) Prerequisites

- Python 3.10+
- AWS account credentials (AWS Academy lab credentials are fine)
- IAM permissions for:
  - EC2: run/describe/terminate instances, key pairs, security groups, addresses
  - SSM: `GetParameter` (for Ubuntu 24.04 AMI lookup)

Typical AWS CLI setup (one-time):

```bash
aws configure
```

Or use a profile and set `AWS_PROFILE` in `.env`.

## 2) Install dependencies

From `be-forum/`:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r provisioning/requirements.txt
```

## 3) Configure environment variables

```bash
cp provisioning/.env.example provisioning/.env
```

Edit `provisioning/.env`.

### Required essentials

- `AWS_REGION`
- `OPEN_PORTS`
- `ROOT_VOLUME_GB`
- `KEY_PAIR_NAME`

### Sizing options

Use one of:
- `INSTANCE_TYPE=t3.medium` (recommended, explicit)
- or `MIN_RAM_GIB=4` (script picks nearest type from internal catalog)

> Note: AWS EC2 cannot set “RAM” directly. RAM is determined by instance type. This script maps RAM requirement to a type when `INSTANCE_TYPE` is omitted.

### Elastic IP

If you already created an Elastic IP, set one of:
- `EIP_ALLOCATION_ID=eipalloc-...` (recommended)
- `EIP_PUBLIC_IP=x.x.x.x`

### Network

- If `VPC_ID`/`SUBNET_ID` are empty, script uses your default VPC/subnet.
- `OPEN_PORTS` is a comma-separated list (TCP, opened to `0.0.0.0/0`).

Example:

```env
AWS_REGION=ap-southeast-1
INSTANCE_TYPE=t3.medium
ROOT_VOLUME_GB=30
OPEN_PORTS=22,80,443,8080
KEY_PAIR_NAME=be-forum-key
CREATE_KEY_PAIR=true
EIP_ALLOCATION_ID=eipalloc-0123456789abcdef0
```

## 4) Run provisioning

```bash
python provisioning/provision_ec2.py
```

### Dry run

```bash
DRY_RUN=true python provisioning/provision_ec2.py
```

## 5) Destroy instance (safe teardown)

```bash
python provisioning/destroy_ec2.py
```

Behavior:
- Resolves target instance by:
  - `DESTROY_INSTANCE_ID` if set, otherwise
  - tags (`INSTANCE_NAME`, `PROJECT_TAG`, `ENV_TAG`)
- Prompts for confirmation unless `DESTROY_FORCE=true`
- Disassociates configured EIP before terminate (if set)
- Terminates instance and waits until fully terminated
- Optional cleanup (env-controlled):
  - release EIP (`RELEASE_EIP_ON_DESTROY=true`)
  - delete key pair (`DELETE_KEY_PAIR_ON_DESTROY=true`)
  - delete local `.pem` (`DELETE_LOCAL_KEY_FILE_ON_DESTROY=true`)

Recommended for safety:

```env
DESTROY_INSTANCE_ID=i-0123456789abcdef0
DESTROY_FORCE=false
```

## 6) What `provision_ec2.py` does

1. Loads config from `.env`
2. Resolves Ubuntu 24.04 AMI from AWS SSM public parameter
3. Ensures key pair exists (can create and save `.pem`)
4. Creates/reuses security group and opens requested ports
5. Launches EC2 instance with encrypted gp3 root volume
6. Installs Docker, Docker Compose plugin, and Git via cloud-init/user-data
7. Associates existing Elastic IP if provided

## 7) Security notes

- Keep private key files secure (`chmod 600` is applied automatically)
- Avoid opening unnecessary ports in `OPEN_PORTS`
- Prefer restricting SSH (`22`) to your IP in production (this script currently opens listed ports to `0.0.0.0/0` for simplicity)
- Use `DESTROY_INSTANCE_ID` for teardown to avoid deleting wrong instances

## 8) Troubleshooting

- `No default VPC found`: set `VPC_ID` and `SUBNET_ID` explicitly.
- `UnauthorizedOperation`: your IAM role/user lacks required permissions.
- `Elastic IP not found`: check `EIP_ALLOCATION_ID` or `EIP_PUBLIC_IP` and region.
- `Key pair not found`: set `CREATE_KEY_PAIR=true` or pre-create key in the same region.
- `Multiple instances matched`: set `DESTROY_INSTANCE_ID` explicitly.

