package com.vo.core;

import com.vo.service.SVNService;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月24日
 *
 */
public class OS {

	private static final String UBUNTU = "ubuntu";
	private static final String WINDOWS = "windows";
	private static final String LINUX = "linux";

	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

	public static boolean isWindows() {
		return OS_NAME.toLowerCase().contains(WINDOWS);
	}

	public static boolean isLinux() {
		return OS_NAME.toLowerCase().contains(LINUX);
	}

	public static boolean isUbuntu() {
		if (!isLinux()) {
			return false;
		}
		final String executeLinux = SVNService.executeLinux("TEST", "lsb_release -a");
		System.out.println("executeLinux = " + executeLinux);

		final boolean contains = executeLinux.toLowerCase().contains(UBUNTU);

		return contains;
	}

}
