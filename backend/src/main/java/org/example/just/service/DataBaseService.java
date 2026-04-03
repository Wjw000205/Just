package org.example.just.service;

import org.example.just.dto.databaseDto.DataBasePageInitInfoVO;
import org.example.just.utils.Result;

public interface DataBaseService {
    Result<DataBasePageInitInfoVO> getPageInitInfo();
}
