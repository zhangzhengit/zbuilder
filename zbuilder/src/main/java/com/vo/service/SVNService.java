package com.vo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.vo.api.BuildService;
import com.vo.core.ZLog2;
import com.vo.enums.SVNCommandEnum;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *	执行SVN相关命令
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
public class SVNService {

	public static final String NEW_LINE = "\r\n";
	public static final String BUILD_END = "构建结束";
	private static final ZLog2 LOG = ZLog2.getInstance();
	private static final ZE ZE = ZES.newZE(Runtime.getRuntime().availableProcessors(), "svnService-Thread-");


	private static String execute0(final String uuid, final SVNCommandEnum commandEnum, final String userName,
			final String password, final String url) {
		LOG.info("开始执行SVN命令,command={},userName={}", commandEnum.getCommand(), userName);

		final String command = commandEnum.getCommand() + " " + url + " --username " + userName + " --password "
				+ password;

		final ExecuteResult result = executeLinux(uuid, command);
		return result.getOutput();
	}

	public static String executeWindows(final String uuid, final String command) {
		LOG.info("command={}", command);

		try {
			final ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
			processBuilder.environment().put("LANG", "en_US.UTF-8"); // 设置 LANG 环境变量为 UTF-8
			final Process process = processBuilder.start();

//			if (exitCode == 0) {
				final InputStream inputStream = process.getInputStream();
				final StringBuilder r = read(uuid, inputStream);
				// FIXME 2023年9月24日 下午4:33:55 zhanghen: sleep 测试
				final int millis = 1;
				Thread.sleep(millis);
//			}
			final int exitCode = process.waitFor();
			System.out.println("exitCode = " + exitCode);

			return r.toString();

		} catch (final IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static ExecuteResult executeLinux(final String uuid, final String command) {
		LOG.info("command={}", command);

		try {
			final ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
			// 不加下面一行，ssh java -jar 会卡死等不到输入
			processBuilder.redirectErrorStream(true);
			final Process process = processBuilder.start();
			final InputStream inputStream = process.getInputStream();
			final StringBuilder r = read(uuid, inputStream);
			final int exitCode = process.waitFor();
			System.out.println("exitCode = " + exitCode);

//			return r.toString();
			final ExecuteResult result = new ExecuteResult(exitCode, r.toString());
			return result;
		} catch (final IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static StringBuilder read(final String uuid, final InputStream inputStream) throws IOException {

		final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
		final StringBuilder r = new StringBuilder();
		while (true) {
			final String readLine = reader.readLine();
			if (readLine != null) {
				System.out.println(readLine);
				BuildService.addOutput(uuid, readLine);
				r.append(readLine).append(NEW_LINE);
			} else {
				break;
			}
		}
		return r;
	}


}
