package com.xinki.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinki.portfolio.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
