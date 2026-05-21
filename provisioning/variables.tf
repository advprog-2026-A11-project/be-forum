variable "aws_region" {
  description = "AWS region for provisioning."
  type        = string
}

variable "aws_profile" {
  description = "Optional AWS shared credentials profile."
  type        = string
  default     = null
}

variable "instance_name" {
  description = "Name tag for the EC2 instance."
  type        = string
  default     = "be-forum-ec2"
}

variable "project_tag" {
  description = "Project tag value."
  type        = string
  default     = "be-forum"
}

variable "env_tag" {
  description = "Environment tag value."
  type        = string
  default     = "dev"
}

variable "instance_type" {
  description = "EC2 instance type (for example t3.medium)."
  type        = string
}

variable "ami_id" {
  description = "AMI ID used by the EC2 instance."
  type        = string
}

variable "root_volume_gb" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_gb >= 8
    error_message = "root_volume_gb must be at least 8 GiB."
  }
}

variable "key_pair_name" {
  description = "Optional SSH key pair name."
  type        = string
  default     = ""
}

variable "vpc_id" {
  description = "Optional VPC ID. Leave empty to use default VPC."
  type        = string
  default     = ""
}

variable "subnet_id" {
  description = "Optional subnet ID. Leave empty to use the first subnet in the selected VPC."
  type        = string
  default     = ""
}

variable "existing_security_group_id" {
  description = "Existing security group ID. Leave empty to create a new security group."
  type        = string
  default     = ""
}

variable "security_group_name" {
  description = "Security group name to use when creating a new group."
  type        = string
  default     = "be-forum-sg"
}

variable "open_ports_csv" {
  description = "Comma-separated TCP ports to open when Terraform creates the security group."
  type        = string
  default     = "22,80,443"

  validation {
    condition = length([
      for p in split(",", var.open_ports_csv) :
      trimspace(p) if trimspace(p) != ""
    ]) > 0
    error_message = "open_ports_csv must contain at least one port."
  }

  validation {
    condition = alltrue([
      for p in split(",", var.open_ports_csv) :
      can(tonumber(trimspace(p))) && tonumber(trimspace(p)) >= 1 && tonumber(trimspace(p)) <= 65535
      if trimspace(p) != ""
    ])
    error_message = "Each port in open_ports_csv must be a number between 1 and 65535."
  }
}

variable "assign_eip" {
  description = "Whether to assign an Elastic IP to the instance."
  type        = bool
  default     = true
}

variable "existing_eip_allocation_id" {
  description = "Existing EIP allocation ID to attach. Leave empty to allocate a new EIP."
  type        = string
  default     = ""
}

variable "extra_tags" {
  description = "Additional tags to merge into all resources."
  type        = map(string)
  default     = {}
}
