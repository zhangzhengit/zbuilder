package com.vo.api;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.vo.conf.Conf;
import com.vo.core.OS;
import com.vo.core.ZContext;
import com.vo.dto.BuildDTO;
import com.vo.dto.DeployDTO;
import com.vo.dto.KillJobDTO;
import com.vo.entity.BuildHistoryEntity;
import com.vo.entity.JobEntity;
import com.vo.entity.NodeEntity;
import com.vo.enums.JobStatusEnum;
import com.vo.enums.SVNCommandEnum;
import com.vo.repository.BuildHistoryRepository;
import com.vo.repository.JobRepository;
import com.vo.repository.NodeRepository;
import com.vo.repository.NodeService;
import com.vo.service.BuildHistoryService;
import com.vo.service.JobService;
import com.vo.service.SVNService;
import com.votool.common.CR;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;

/**
 * 构建相关操作
 *
 * @author zhangzhen
 * @date 2023年9月24日
 *
 */
public class BuildService {
	private static final  ZE ze = ZES.newZE();

	private static final BuildHistoryRepository BUILD_HISTORY_REPOSITORY = ZContext.getBean(BuildHistoryRepository.class);
	private static final BuildHistoryService BUILD_HISTORY_SERVICE = ZContext.getBean(BuildHistoryService.class);
	private static final NodeRepository NODE_REPOSITORY = ZContext.getBean(NodeRepository.class);
	private static final JobService JOB_SERVICE = ZContext.getBean(JobService.class);
	private static final JobRepository JOB_REPOSITORY = ZContext.getBean(JobRepository.class);

	private static final ConcurrentMap<String, Queue<String>> MAP = Maps.newConcurrentMap();
	public static final ConcurrentMap<String, Boolean> MAP_DONE = Maps.newConcurrentMap();

	public synchronized static CR<Object> build(final BuildDTO buildDTO) {

		final Integer jobId = buildDTO.getJobId();

		final JobEntity jobEntity = JOB_REPOSITORY.findById(jobId);
		if (jobEntity == null) {
			return CR.error("job不存在");
		}

		final JobStatusEnum statusEnum = JobStatusEnum.valueOfStatus(jobEntity.getStatus());
		if (statusEnum != JobStatusEnum.IDLE) {
			return CR.error("job非空闲状态，不可以执行当前操作,id = " + jobId);
		}
		jobEntity.setStatus(JobStatusEnum.BUILD.getStatus());
		JOB_REPOSITORY.update(jobEntity);


		final Integer maxVersion = BUILD_HISTORY_SERVICE.findMaxVersion(jobId);

		final String uuid = UUID.randomUUID().toString();
		ze.executeInQueue(() -> {

			final BuildHistoryEntity insertBHEntity = new BuildHistoryEntity();
			insertBHEntity.setCreateTime(new Date());
			insertBHEntity.setJobId(jobId);
			insertBHEntity.setUuid(uuid);
			insertBHEntity.setRemark(buildDTO.getRemark());
			insertBHEntity.setVersion(maxVersion + 1);
			insertBHEntity.setResult(BuildHistoryResultEnum.BUILDING.name());
			final BuildHistoryEntity createOne = BUILD_HISTORY_SERVICE.createOne(insertBHEntity);

			final BuildHistoryEntity appendOutputBHEntity = new BuildHistoryEntity();
			appendOutputBHEntity.setUuid(uuid);
			appendOutputBHEntity.setJobId(jobId);

			final String mvnResult = OS.isLinux() ? buildLinux(uuid, jobEntity)
					: OS.isWindows() ? buildWindows(uuid, jobEntity) : null;
			if (mvnResult == null) {
				throw new IllegalArgumentException("平台不支持，支持平台：windwos/linux");
			}

			try {
				appendOutputBHEntity.setOutput(mvnResult);
				if (mvnResult.contains("[ERROR]")) {
					appendOutputBHEntity.setResult(BuildHistoryResultEnum.ERROR.name());
				} else {
					appendOutputBHEntity.setResult(BuildHistoryResultEnum.SUCCESS.name());
				}
			} catch (final Exception e) {
				jobEntity.setStatus(JobStatusEnum.IDLE.getStatus());
				JOB_REPOSITORY.update(jobEntity);
				createOne.setResult(BuildHistoryResultEnum.ERROR.name());
				BUILD_HISTORY_REPOSITORY.update(createOne);
			} finally {
				jobEntity.setStatus(JobStatusEnum.IDLE.getStatus());
				JOB_REPOSITORY.update(jobEntity);
			}

			appendOutputBHEntity.setCreateTime(new Date());

			BUILD_HISTORY_SERVICE.appendOutputAndInsertResult(appendOutputBHEntity);

		});

		return CR.ok(uuid);
	}


	public static void copy(final File source, final File target) {
		createNewFile(source, target);

//		createNewFile(source, target);
		// copy app-1.0-SNAPSHOT.jar E:\
		final String executeWindows = SVNService.executeWindows("A", "copy " + source.getAbsolutePath() + " " + target.getAbsolutePath());

		System.out.println("executeWindows = " + executeWindows);

	}


	private static void createNewFile(final File source, final File target) {
		if (!source.exists()) {
			try {
				source.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (!target.exists()) {
			try {
				target.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String buildWindows(final String uuid, final JobEntity jobEntity) {
		final Conf conf = ZContext.getBean(Conf.class);
		final String workspace = conf.getWorkspace();
		System.out.println("workspace = " + workspace);

		addOutput(uuid, "工作区：" + workspace);

		final File workspaceFile = new File(workspace);

		if (!workspaceFile.exists()) {
			if (!workspaceFile.mkdirs()) {
				addOutput(uuid, "构建失败，新建工作位失败：workspace = " + workspace);
				return "构建失败，新建工作位失败：workspace = " + workspace;
			}

			addOutput(uuid, "新建工作区：" + workspace);
		}

		final String absolutePath = workspaceFile.getAbsolutePath();
		System.out.println("absolutePath = " + absolutePath);

		final String checkout = "cd /d " + absolutePath + " && " + SVNCommandEnum.CHECKOUT.getCommand() + " "
				+ jobEntity.getRepositoryUrl() + " --username " + jobEntity.getUserName() + " --password "
				+ jobEntity.getPassword();

		addOutput(uuid, "开始拉取代码：" + checkout);

		final String checkoutResult = SVNService.executeWindows(uuid, checkout);
		System.out.println("checkoutResult = " + checkoutResult);

		final String mvn = "mvn -f " + absolutePath.trim() + File.separator + jobEntity.getProjectName().trim() + " -B "
				+ jobEntity.getBuildCommand();


		addOutput(uuid, "===============================================================================");
		addOutput(uuid, "[构建开始] " + LocalDateTime.now());
		addOutput(uuid, "===============================================================================");
		final long t1 = System.currentTimeMillis();
		final String mvnResult = SVNService.executeWindows(uuid, mvn);
		final long t2 = System.currentTimeMillis();
		addOutput(uuid, "===============================================================================");
		addOutput(uuid, "[构建结束] " + LocalDateTime.now() + "\t" + "耗时 " + (t2 - t1) + " 毫秒");
		addOutput(uuid, "===============================================================================");

		// sb_jenkins_test_20230923-1.0-SNAPSHOT.jar
		final String sourceDir = absolutePath.trim() + File.separator + jobEntity.getProjectName().trim() + File.separator
				+ "target" + File.separator;
		final File jarFile = findFile(sourceDir, "^" + jobEntity.getProjectName().trim() + ".*" + "\\.jar$");

		addOutput(uuid, "目标文件 =  " + jarFile.getAbsolutePath());
		addOutput(uuid, "开始拷贝目标文件到执行目录...");

		final String jarDir = workspaceFile.getAbsolutePath() + File.separator + "jar";
		final File jardiRF = new File(jarDir);
		if(!jardiRF.exists()) {
			jardiRF.mkdirs();
		}

		final String jarpath = StrUtil.removeAll(jarFile.getAbsolutePath(), jarFile.getName());
		final String robocopy =  "chcp 65001 && robocopy " + jarpath + " " + jarDir + " " + jarFile.getName();

		System.out.println("robocopy = " + robocopy);

		addOutput(uuid, robocopy);
		final String executeWindows = SVNService.executeWindows(uuid, robocopy);
		addOutput(uuid, executeWindows);
		addOutput(uuid, "===============================================================================");
		addOutput(uuid, "拷贝目标文件到执行目录完成");

		// 结束标识
		addOutput(uuid, SVNService.BUILD_END);

		return mvnResult;
	}

	private static File findFile(final String dir, final String targetFileName) {
		final File dirF = checkDir(dir);
		if (dirF == null) {
			throw new IllegalArgumentException("工作区下未找到文件");
		}

		final File[] ls = dirF.listFiles();
		for (final File file : ls) {
			if (file.getName().matches(targetFileName)) {
				System.out.println("找到jar = " + file.getAbsolutePath());
				return file;
			}
		}
//		final String input = "sb_jenkins_test_20230923_example.jar";
//		final boolean isMatch = input.matches("^sb_jenkins_test_20230923.*\\.jar$");
//		System.out.println(isMatch); // 输出: true

//		return null;
		throw new IllegalArgumentException("工作区下未找到文件");
	}

	private static File checkDir(final String dir) {
		try {

			final File dirF = new File(dir + File.separator);
			if (!dirF.exists()) {
				final boolean mkdirs = dirF.mkdirs();
				if (!mkdirs) {
					return null;
				}
			}
			return dirF;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String buildLinux(final String uuid, final JobEntity jobEntity) {
		final Conf conf = ZContext.getBean(Conf.class);
		final String workspace = conf.getWorkspace();
		System.out.println("workspace = " + workspace);

		addOutput(uuid, "工作区：" + workspace);

		final File workspaceFile = new File(workspace);

		if (!workspaceFile.exists()) {
			if (!workspaceFile.mkdirs()) {
				addOutput(uuid, "构建失败，新建工作位失败：workspace = " + workspace);
				return "构建失败，新建工作位失败：workspace = " + workspace;
			}

			addOutput(uuid, "新建工作区：" + workspace);
		}


		final String workspaceAbsolutePath = workspaceFile.getAbsolutePath();
		System.out.println("absolutePath = " + workspaceAbsolutePath);


		final String checkout = "cd " + workspaceAbsolutePath + " && " + SVNCommandEnum.CHECKOUT.getCommand()
		 	 + " " + jobEntity.getRepositoryUrl() + " --username " + jobEntity.getUserName()
		 	 + " --password " + jobEntity.getPassword();

		addOutput(uuid, "开始拉取代码：" + checkout);

		final String checkoutResult = SVNService.executeLinux(uuid, checkout);
		System.out.println("checkoutResult = " + checkoutResult);

		final String mvn = "mvn -f " + workspaceAbsolutePath.trim() + "/" + jobEntity.getProjectName().trim() + " -B "
				+ jobEntity.getBuildCommand();


		addOutput(uuid, "===============================================================================");
		addOutput(uuid, "[构建开始] " + LocalDateTime.now());
		addOutput(uuid, "===============================================================================");
		final long t1 = System.currentTimeMillis();
		final String mvnResult = SVNService.executeLinux(uuid, mvn);
		final long t2 = System.currentTimeMillis();
		addOutput(uuid, "===============================================================================");
		addOutput(uuid, "[构建结束] " + LocalDateTime.now() + "\t" + "耗时 " + (t2 - t1) + " 毫秒");
		addOutput(uuid, "===============================================================================");

		// sb_jenkins_test_20230923-1.0-SNAPSHOT.jar
		final String sourceDir = workspaceAbsolutePath.trim() + File.separator + jobEntity.getProjectName().trim() + File.separator
				+ "target" + File.separator;
		final File jarFile = findFile(sourceDir, "^" + jobEntity.getProjectName().trim() + ".*" + "\\.jar$");

		addOutput(uuid, "目标文件 =  " + jarFile.getAbsolutePath());
		addOutput(uuid, "开始拷贝目标文件到执行目录...");

		final String jarDir = workspaceFile.getAbsolutePath() + File.separator + "jar"  + File.separator + StrUtil.removeAll(jarFile.getName(), ".jar");
		final File jardiRF = new File(jarDir);
		if(!jardiRF.exists()) {
			jardiRF.mkdirs();
		}

		final String robocopy =  "cp -vf " + jarFile.getAbsolutePath() + " " + jarDir ;

		System.out.println("cp = " + robocopy);

		addOutput(uuid, robocopy);
		final String executeWindows = SVNService.executeLinux(uuid, robocopy);
		addOutput(uuid, executeWindows);
		addOutput(uuid, "===============================================================================");
		addOutput(uuid, "拷贝目标文件到执行目录完成");

		// 结束标识
		addOutput(uuid, SVNService.BUILD_END);

		return mvnResult;
	}


	public  static String getOutput(final String uuid) {

		final Queue<String> queue = MAP.get(uuid);
		if (queue == null) {
			return null;
		}

		final String poll = queue.poll();
		return poll;
	}

	public  static void addOutput(final String uuid, final String output) {
		final Queue<String> queue = MAP.get(uuid);
		if (queue == null) {
			final Queue<String> queueX = new LinkedList<>();
			queueX.add(output);
			MAP.put(uuid, queueX);
		} else {
			queue.add(output);
		}
	}
	static 	final ConcurrentMap<String, String> deployLockMap = Maps.newConcurrentMap();

	public static CR<Object> deploy(final DeployDTO deployDTO) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "BuildService.deploy()");

		final String key = "deploy-" + String.valueOf(deployDTO.getHId());
		synchronized (key) {
			final String v = deployLockMap.get(key);
			if (v != null) {
				return CR.error("job正在部署中，请稍后重试");
			}
		}

		final NodeService nodeService = ZContext.getBean(NodeService.class);
//		nodeService.kill
		final KillJobDTO killJobDTO = new KillJobDTO();

		final BuildHistoryRepository buildHistoryRepository = ZContext.getBean(BuildHistoryRepository.class);
		final BuildHistoryEntity buildHistoryEntity = buildHistoryRepository.findById(deployDTO.getHId());

		killJobDTO.setJobId(buildHistoryEntity.getJobId());
		killJobDTO.setNodeId(null);

		final List<NodeEntity> nodeList = JOB_SERVICE.findNodeInfoById(buildHistoryEntity.getJobId());

		try {
			deployLockMap.put(key, "正在部署中");
			final CR<Object> cr = deploy0(deployDTO);
			return cr;
		} catch (final Exception e) {
			e.printStackTrace();
			deployLockMap.remove(key);
		} finally {
			deployLockMap.remove(key);
		}

		return CR.ok();
	}


	private static CR<Object> deploy0(final DeployDTO deployDTO) {
		final BuildHistoryRepository buildHistoryRepository = ZContext.getBean(BuildHistoryRepository.class);
//		final List<BuildHistoryEntity> list = buildHistoryRepository.findByUuid(uuid);
		final BuildHistoryEntity buildHistoryEntity = buildHistoryRepository.findById(deployDTO.getHId());
		if (buildHistoryEntity==null) {
			return CR.error("构建信息不存在,id = " + deployDTO.getHId());
		}

		final Integer jobId = buildHistoryEntity.getJobId();
		final JobEntity jobEntity = JOB_REPOSITORY.findById(jobId);

		final File jarFile = findJarFile(jobEntity);
		if (Objects.isNull(jarFile)) {
			return CR.error("版本[" + deployDTO.getVersion() + "]不存在");
		}

		final List<Integer> nodeIdList = JOB_SERVICE.parseNodeIdArray(jobEntity);
		for (final Integer id : nodeIdList) {
			final NodeEntity nodeEntity = NODE_REPOSITORY.findById(id);

//			ssh -fv root@192.168.1.5 "nohup java -jar /root//gangdan/sb_jenkins_test_20230923/
//			target/sb_jenkins_test_20230923-1.0-SNAPSHOT.jar >/dev/null 2>&1 &"

//			final String jarDir = new File(ZContext.getBean(Conf.class).getWorkspace()).getAbsolutePath() + File.separator + "jar"  + File.separator + StrUtil.removeAll(jarFile.getName(), ".jar");

			// 1 把jar文件scp到节点机器上
			//ssh -v root@192.168.1.5 'mkdir -p /root/gangdan/jar/sb_jenkins'
//			scp sb_jenkins_test_20230923-1.0-SNAPSHOT.jar root@192.168.1.5:/root/gangdan/jar/sb_jenkins

			final String mkdirP = "ssh -v root@" + nodeEntity.getIp() + " " + "\'mkdir -p "
					+ jobEntity.getTargetDirectory() + File.separator + "jar" + File.separator
					+ jobEntity.getProjectName() + "\'";
			System.out.println("mkdirP = " + mkdirP);
			final String mkdirPOutput = SVNService.executeLinux(buildHistoryEntity.getUuid(), mkdirP);
			System.out.println("mkdirPOutput" + mkdirPOutput);
			// OK


			final String scp = "scp " + jarFile.getAbsolutePath()
					 + " root@" + nodeEntity.getIp()
					 + ":" + jobEntity.getTargetDirectory()
					 + File.separator + "jar" +  File.separator
					 + jobEntity.getProjectName()
					 ;
			// OK
			System.out.println("scp = " + scp);
			final String scpOutput = SVNService.executeLinux(buildHistoryEntity.getUuid(), scp);
			System.out.println("scpOutput = " + scpOutput);

			// 2 ssh执行 java -jar
			// FIXME 2023年10月2日 下午8:19:35 zhanghen: 改为如下：先cd 然后&& java -jar 才可以正常读取jar同目录的配置文件
			//  ssh -fv root@192.168.1.10 'cd /root/gangdan//jar/sb_zframework_TEST/ && nohup java -jar /root/gangdan//jar/sb_zframework_TEST/sb_zframework_TEST-1.0-SNAPSHOT.jar >> /root/gangdan/jar/sb_zframework_TEST/app.log &'
			final String  deploy = "ssh -fv root@" + nodeEntity.getIp()
				+ " " + "\'nohup java -jar "
				+ jobEntity.getTargetDirectory()
				 + File.separator + "jar" +  File.separator
				 + jobEntity.getProjectName() + File.separator + jarFile.getName()
				+ " >> " + jobEntity.getLogFilePath() + " &'"
				;

			// FIXME 2023年10月2日 下午9:02:12 zhanghen: XXX server.port 写死在java -jar -Dserver.port 里？
			// 还是界面新增输入框，可以自定义启动命令？如：cd /root/xx && java -jar -Dserver.port ..........
			final String deployCD1 = "ssh -fv root@" + nodeEntity.getIp()
				+ " " + "\'cd "
				+ jobEntity.getTargetDirectory()
				+ File.separator + "jar"
				+ File.separator
				+ jobEntity.getProjectName()
				+ " && nohup java -jar "
				+ jarFile.getName()
				+ " >> " + jobEntity.getLogFilePath() + " &'"
				;


			System.out.println("deploy = " + deployCD1);

			final String deployOutput = SVNService.executeLinux(buildHistoryEntity.getUuid(), deployCD1);
			System.out.println("deployOutput = " + deployOutput);

		}

		return CR.ok();
	}

	public  static File findJarFile(final JobEntity jobEntity) {
		final String absolutePath = new File(ZContext.getBean(Conf.class).getWorkspace()).getAbsolutePath();
		final String sourceDir = absolutePath.trim() + File.separator + jobEntity.getProjectName().trim()
				+ File.separator + "target" + File.separator;
		final File jarFile = findFile(sourceDir, "^" + jobEntity.getProjectName().trim() + ".*" + "\\.jar$");
		return jarFile;
	}


	public static void copyToBackups(final File jarFile) {

	}




































}

