package org.example.just.dto.databaseDto;

import lombok.Data;
import org.example.just.service.imp.DataBaseServiceImpl;

import java.util.List;

@Data
public class DataBasePageInitInfoVO {
    List<DataBaseServiceImpl.ClassificationTreeNode>  tree;
}
