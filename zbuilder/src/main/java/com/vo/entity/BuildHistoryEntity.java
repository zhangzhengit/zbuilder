package com.vo.entity;

import java.util.Date;

import com.vo.ZDateFormat;
import com.vo.ZDateFormatEnum;
import com.vo.ZID;
import com.vo.anno.ZEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月26日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZEntity(tableName = "build_history")
public class BuildHistoryEntity {

	@ZID
	private Integer id;
	private Integer jobId;
	private String uuid;
	private String output;
	private String remark;
	@ZDateFormat(format = ZDateFormatEnum.YYYY_MM_DD_HH_MM_SS)
	private Date createTime;
	private Integer version;
	private String result;


}
