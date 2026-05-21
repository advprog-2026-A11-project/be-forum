# EC2 Provisioning with Terraform

This folder provisions AWS infrastructure through Terraform using environment variables.

Provisioning supports:
1. Create a security group with configurable open ports.
2. Create an EC2 VM with configurable size and AMI.
3. Attach the VM to either:
   - an existing security group, or
   - a new security group created by Terraform.
4. Assign an Elastic IP to the VM:
   - use an existing allocation ID, or
   - create a new EIP and attach it.
5. Destroy the VM and related Terraform-managed resources.

## Prerequisites

- Terraform `>= 1.5`
- AWS credentials with EC2/VPC/EIP permissions

## Configure via environment variables

Copy:

```bash
cp provisioning/.env.example provisioning/.env
```

`provisioning/.env.example` uses `TF_VAR_*` names so Terraform reads values directly from env vars.

Load env vars.

PowerShell:

```powershell
Get-Content provisioning/.env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $name, $value = $_ -split '=', 2
  Set-Item -Path "Env:$name" -Value $value
}
```

Bash:

```bash
set -a
source provisioning/.env
set +a
```

## Create infrastructure

```bash
cd provisioning
terraform init
terraform plan -out tfplan
terraform apply tfplan
```

## Destroy infrastructure (including VM)

```bash
cd provisioning
terraform destroy
```

Optional non-interactive destroy:

```bash
cd provisioning
terraform destroy -auto-approve
```

## Notes

- If `TF_VAR_existing_security_group_id` is empty, Terraform creates a new security group with ports from `TF_VAR_open_ports_csv`.
- If `TF_VAR_existing_eip_allocation_id` is empty and `TF_VAR_assign_eip=true`, Terraform allocates a new EIP and attaches it.
- If `TF_VAR_vpc_id` and `TF_VAR_subnet_id` are empty, Terraform uses the default VPC and the first subnet in that VPC.
