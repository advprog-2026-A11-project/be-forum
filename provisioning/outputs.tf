output "instance_id" {
  description = "Provisioned EC2 instance ID."
  value       = aws_instance.vm.id
}

output "instance_public_ip" {
  description = "EC2 instance public IP (after EIP association if enabled)."
  value       = aws_instance.vm.public_ip
}

output "instance_private_ip" {
  description = "EC2 instance private IP."
  value       = aws_instance.vm.private_ip
}

output "security_group_id" {
  description = "Security group attached to the instance."
  value       = local.effective_security_group_id
}

output "eip_allocation_id" {
  description = "Elastic IP allocation ID attached to the instance."
  value       = var.assign_eip ? local.effective_eip_allocation_id : null
}

output "elastic_ip" {
  description = "Elastic IP public address attached to the instance."
  value = var.assign_eip ? (
    local.create_eip ? aws_eip.vm[0].public_ip : data.aws_eip.existing[0].public_ip
  ) : null
}
