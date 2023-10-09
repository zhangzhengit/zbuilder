package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行svn命令后，控制台输出的结果
 *
 * @author zhangzhen
 * @date 2023年9月24日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SVNResult {

	private String uuid;

	private String result;

}
