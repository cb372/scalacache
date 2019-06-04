PATH := ./redis-git/src:${PATH}

# CLUSTER REDIS NODES
define REDIS_CLUSTER_NODE1_CONF
daemonize yes
port 7000
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node1.pid
logfile /tmp/redis_cluster_node1.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node1.conf
endef

define REDIS_CLUSTER_NODE2_CONF
daemonize yes
port 7001
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node2.pid
logfile /tmp/redis_cluster_node2.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node2.conf
endef

define REDIS_CLUSTER_NODE3_CONF
daemonize yes
port 7002
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node3.pid
logfile /tmp/redis_cluster_node3.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node3.conf
endef

define REDIS_CLUSTER_NODE4_CONF
daemonize yes
port 7003
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node4.pid
logfile /tmp/redis_cluster_node4.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node4.conf
endef

define REDIS_CLUSTER_NODE5_CONF
daemonize yes
port 7004
cluster-node-timeout 5000
pidfile /tmp/redis_cluster_node5.pid
logfile /tmp/redis_cluster_node5.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node5.conf
endef

define REDIS_CLUSTER_NODE6_CONF
daemonize yes
port 7005
cluster-node-timeout 5000
pidfile /tmp/redis_cluster_node6.pid
logfile /tmp/redis_cluster_node6.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node6.conf
endef

export REDIS_CLUSTER_NODE1_CONF
export REDIS_CLUSTER_NODE2_CONF
export REDIS_CLUSTER_NODE3_CONF
export REDIS_CLUSTER_NODE4_CONF
export REDIS_CLUSTER_NODE5_CONF
export REDIS_CLUSTER_NODE6_CONF


start: cleanup
	echo "$$REDIS_CLUSTER_NODE1_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE2_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE3_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE4_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE5_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE6_CONF" | redis-server -

cleanup:
	- rm -vf /tmp/redis_cluster_node*.conf 2>/dev/null
	- rm dump.rdb appendonly.aof - 2>/dev/null

stop:
	kill `cat /tmp/redis_cluster_node1.pid` || true
	kill `cat /tmp/redis_cluster_node2.pid` || true
	kill `cat /tmp/redis_cluster_node3.pid` || true
	kill `cat /tmp/redis_cluster_node4.pid` || true
	kill `cat /tmp/redis_cluster_node5.pid` || true
	kill `cat /tmp/redis_cluster_node6.pid` || true
	rm -f /tmp/redis_cluster_node1.conf
	rm -f /tmp/redis_cluster_node2.conf
	rm -f /tmp/redis_cluster_node3.conf
	rm -f /tmp/redis_cluster_node4.conf
	rm -f /tmp/redis_cluster_node5.conf
	rm -f /tmp/redis_cluster_node6.conf

travis-run:
	make start
	echo yes | ruby ./redis-trib.rb create --replicas 1 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005

travis-install:
	wget https://raw.githubusercontent.com/antirez/redis/4.0.6/src/redis-trib.rb -O redis-trib.rb
	gem install redis
