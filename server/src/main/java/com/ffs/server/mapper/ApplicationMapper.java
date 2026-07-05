package com.ffs.server.mapper;

import com.ffs.server.model.entity.Application;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApplicationMapper {
    Application findById(Long id);
    Application findByName(String name);
    List<Application> findAll();
    int insert(Application app);
}
