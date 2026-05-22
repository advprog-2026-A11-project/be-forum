provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile
}

locals {
  tags = merge(
    {
      Project     = var.project_tag
      Environment = var.env_tag
    },
    var.extra_tags,
  )

  create_security_group = trimspace(var.existing_security_group_id) == ""
  parsed_open_ports = distinct([
    for p in split(",", var.open_ports_csv) :
    tonumber(trimspace(p)) if trimspace(p) != ""
  ])
}

data "aws_vpc" "default" {
  count   = var.vpc_id == "" ? 1 : 0
  default = true
}

locals {
  effective_vpc_id = var.vpc_id != "" ? var.vpc_id : data.aws_vpc.default[0].id
}

data "aws_subnets" "in_vpc" {
  count = var.subnet_id == "" ? 1 : 0

  filter {
    name   = "vpc-id"
    values = [local.effective_vpc_id]
  }
}

locals {
  effective_subnet_id = var.subnet_id != "" ? var.subnet_id : data.aws_subnets.in_vpc[0].ids[0]
}

resource "aws_security_group" "forum" {
  count = local.create_security_group ? 1 : 0

  name        = var.security_group_name
  description = "Security group for be-forum EC2"
  vpc_id      = local.effective_vpc_id

  dynamic "ingress" {
    for_each = toset(local.parsed_open_ports)
    content {
      description = "Open TCP port ${ingress.value}"
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.tags, { Name = var.security_group_name })
}

locals {
  effective_security_group_id = local.create_security_group ? aws_security_group.forum[0].id : trimspace(var.existing_security_group_id)
}

resource "aws_instance" "vm" {
  ami           = var.ami_id
  instance_type = var.instance_type
  subnet_id     = local.effective_subnet_id
  key_name      = var.key_pair_name != "" ? var.key_pair_name : null

  vpc_security_group_ids = [local.effective_security_group_id]
  user_data = <<-EOT
    #!/bin/bash
    set -euxo pipefail

    if command -v apt-get >/dev/null 2>&1; then
      export DEBIAN_FRONTEND=noninteractive
      apt-get update
      apt-get install -y ca-certificates curl git
    elif command -v dnf >/dev/null 2>&1; then
      dnf makecache
      dnf install -y ca-certificates curl git
    elif command -v yum >/dev/null 2>&1; then
      yum makecache
      yum install -y ca-certificates curl git
    else
      echo "No supported package manager found." >&2
      exit 1
    fi

    curl -fsSL https://get.docker.com | sh

    systemctl enable --now docker
    if id -u ubuntu >/dev/null 2>&1; then
      usermod -aG docker ubuntu
    fi

    if ! docker compose version >/dev/null 2>&1; then
      if command -v apt-get >/dev/null 2>&1; then
        apt-get install -y docker-compose-plugin || apt-get install -y docker-compose-v2
      elif command -v dnf >/dev/null 2>&1; then
        dnf install -y docker-compose-plugin
      elif command -v yum >/dev/null 2>&1; then
        yum install -y docker-compose-plugin
      fi
    fi
  EOT

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_gb
    encrypted             = true
    delete_on_termination = true
  }

  tags = merge(local.tags, { Name = var.instance_name })

  lifecycle {
    precondition {
      condition     = var.subnet_id != "" || try(length(data.aws_subnets.in_vpc[0].ids) > 0, false)
      error_message = "No subnet was found in the selected VPC. Set subnet_id explicitly."
    }
  }
}

locals {
  create_eip                  = var.assign_eip && trimspace(var.existing_eip_allocation_id) == ""
  use_existing_eip_allocation = var.assign_eip && trimspace(var.existing_eip_allocation_id) != ""
}

resource "aws_eip" "vm" {
  count  = local.create_eip ? 1 : 0
  domain = "vpc"
  tags   = merge(local.tags, { Name = "${var.instance_name}-eip" })
}

data "aws_eip" "existing" {
  count = local.use_existing_eip_allocation ? 1 : 0
  id    = trimspace(var.existing_eip_allocation_id)
}

locals {
  effective_eip_allocation_id = local.create_eip ? aws_eip.vm[0].allocation_id : trimspace(var.existing_eip_allocation_id)
}

resource "aws_eip_association" "vm" {
  count         = var.assign_eip ? 1 : 0
  instance_id   = aws_instance.vm.id
  allocation_id = local.effective_eip_allocation_id
}
