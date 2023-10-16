package com.vo;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.vo.conf.Conf;
import com.vo.core.SocketServer;
import com.vo.core.ZClass;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.service.ExecuteResult;
import com.vo.service.SVNService;
import com.vo.starter.ZRepositoryStarter;

/**
 * 写一个自动化部署工具
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
// FIXME 2023年10月6日 下午10:07:30 zhanghen: TODO	仔细测试，以及写以下问题
/*
 * 1 每次构建后，jar包rename为 app-version.jar 的形式
 * 2 每次打出的jar包，都复制到backups目录下，jar包存储到DB？
 * 3 构建输出不止输出maven日志，所有addOutput中的内容都输出
 * 4 支持git仓库，job表加字段，
 * 5 每次部署前，先kill 进程则java -jar启动。否则报错端口号已在使用
 * 6 支持界面新增job（在做了）、编辑job
 * 7 加入登录校验，用户区分admin和普通用户，用户由admin分发和指定权限（新增/编辑/删除job等等）
 * 8 部署指定版本时，先做校验：查找backups目录下文件是否存在等等，然后给出具体提示
 * 9 不只构建，其他耗时长的操作(部署等等)都改为实时输出程序日志
 * 10 代码目录下的resources下的文件，都复制到jar/project_name/下
 * 11 在运行此程序的机器上自动install jdk maven svn git 等，并且自动执行ssh-keygen -t rsa
 * 		并且在新增节点后，自动执行ssh-copy-id root@新节点ip，如有异常都提示出来
 * 12 去除windows支持，仅支持linux(ubuntu)，下面main方法放开注释
 * 13 判断各种可能的null
 *
 */
public class SbGangdanApplication {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void main(final String[] args) {
//		if (!OS.isUbuntu()) {
//			LOG.error("仅测试了ubuntu系统，即将退出程序");
//			System.exit(0);
//		}

		final String scanPackageName = "com.vo";
		startZRepository(scanPackageName);
		ZApplication.run(scanPackageName, true, args);

		final Conf conf = ZContext.getBean(Conf.class);
		final String command = conf.getInstall();

//		LOG.info("开始自动安装软件,command={}", command);
//		final ExecuteResult commandResult = SVNService.executeLinux("install", command);
//
//		if (!commandResult.succeeded()) {
//			LOG.info("安装软件失败，本应用自动关闭。自动安装软件输出={}", commandResult.getOutput());
//			System.exit(0);
//		}
//
//		LOG.info("自动安装软件输出={}", commandResult.getOutput());

		final int port = conf.getWebSocketPort();
		LOG.info("开始启动websocketServer,port={}", port);
		final SocketServer server = new SocketServer(port);
		ZContext.addBean(SocketServer.class, server);
		server.start();
		LOG.info("成功启动websocketServer,port={}", port);


	}

	public static void startZRepository(final String scanPackageName) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "SbZframeworkTestApplication.startZRepository()");

		final Map<Class, ZClass> clsMap = ZRepositoryStarter.startZRepository(scanPackageName);
		final Set<Entry<Class, ZClass>> es = clsMap.entrySet();
		for (final Entry<Class, ZClass> entry : es) {
			System.out.println("zClass.name = " + entry.getKey().getCanonicalName() + "\t" + "value = " +entry.getValue().getName());

			LOG.info("开始注入实现类[{}]", entry.getKey().getCanonicalName());

			ZContext.addBean(entry.getKey().getCanonicalName(), entry.getValue().newInstance());

			LOG.info("注入实现类[{}]成功", entry.getKey().getCanonicalName());
		}
	}
}
