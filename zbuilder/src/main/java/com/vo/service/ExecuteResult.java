package com.vo.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProcessBuilder 执行命令的结果
 *
 * @author zhangzhen
 * @date 2023年10月9日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteResult {

	/**
	 * 0 表示正常退出
	 */
	public static final int OK = 0;

	public boolean succeeded() {
		return this.getCode() == OK;
	}

	private int code;
	private String output;

}
