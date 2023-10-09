package com.vo.conf;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具配置
 *
 * @author zhangzhen
 * @date 2023年9月24日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "build")
public class Conf {

	/**
	 * 构建工作区，在此目录运行代码检出、构建等操作
	 */
	@ZNotNull
	private String workspace;

	/**
	 * 备份目录，备份构建出的jar包程序
	 */
	@ZNotNull
	private String backups;

}
