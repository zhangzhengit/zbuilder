package com.vo.entity;

import java.util.Date;

import com.vo.ZDateFormat;
import com.vo.ZDateFormatEnum;
import com.vo.ZID;
import com.vo.anno.ZEntity;
import com.vo.enums.JobStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZEntity(tableName = "job")
public class JobEntity {

	@ZID
	private Integer id;
	private Integer appPort;
	private String repositoryUrl;
	private String userName;
	private String password;
	private String buildCommand;
	private String targetDirectory;
	private String projectName;

	/**
	 * @see JobStatusEnum
	 */
	private Integer status;

	/**
	 * 多个ID，用,分开
	 */
	private String targetNodeId;

	/**
	 * 日志文件目录，如:/root/jar/app.log
	 */
	private String logFilePath;

//	@ZDateFormat(format = ZDateFormatEnum.YYYY_MM_DD)
	@ZDateFormat(format = ZDateFormatEnum.YYYY_MM_DD_HH_MM_SS)
	private Date createTime;

}
