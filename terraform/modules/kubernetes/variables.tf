variable "name" {
  description = "Name of the kubernetes cluster"
}

variable "network" {
  description = "The network the cluster resides in"
}

variable "node-count" {
  description = "The number of nodes in the cluster, per zone"
  default     = 0
}

variable "master-version" {
  description = "The version of the K8s master"
}

variable "node-pool-version" {
  description = "The version of the nodes in the K8s node pool"
}
