
自动化部署
使用步骤
	1 java -jar 启动本程序 在 X机器上
		X机器上需要 install jdk maven svn 
		zframework.properties 里配置好
			build.workspace		
			build.backups
						
	2 执行 ssh-kengen -t rsa
	
	3 在界面配置节点 A B C 
	
	4 执行 ssh-copy-id root@A/B/C/本机
	
	5 配置job：svn地址、用户名密码、在节点上的执行目录/日志文件目录等等
	
	6 点击按钮： 构建、部署 等 即可正常使用 