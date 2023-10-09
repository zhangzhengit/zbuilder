package com.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
@Getter
@AllArgsConstructor
public enum SVNCommandEnum {

	CHECKOUT("svn checkout","检出"),

	LOG_V("svn log -v","查看提交日志"),

	;


	private String command;
	private String desc;

}
