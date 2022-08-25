package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.common.exception.redis.CacheService;
import com.heima.common.exception.schedule.ScheduleConstants;
import com.heima.model.common.schedule.dtos.Task;
import com.heima.model.common.schedule.entity.Taskinfo;
import com.heima.model.common.schedule.entity.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;
    @Autowired
    private CacheService cacheService;

    /**
     * 添加延时任务
     *
     * @param task 任务对象
     * @return
     */
    @Override
    public long addTask(Task task) {
        // 保存到数据库中
        boolean flag = addTaskToDb(task);
        // 保存到Redis中
        if (flag) {
            addTaskToRedis(task);
        }
        return task.getTaskId();
    }


    private void addTaskToRedis(Task task) {
        // 任务类型+优先级
        String key = task.getTaskType() + "_" + task.getPriority();

        //指定时间 1分钟后
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        long scheduleTime = calendar.getTimeInMillis();

        // 根据时间判断是否为延迟
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            // 小于当前时间
            cacheService.lLeftPush(ScheduleConstants.TOPIC + "_" + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= scheduleTime) {
            // 大于当前时间并且小于指定时间
            cacheService.lLeftPush(ScheduleConstants.FUTURE + "_" + key, JSON.toJSONString(task));
        }


    }

    private boolean addTaskToDb(Task task) {

        boolean flag = false;

        try {
            // 保存任务
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            // 设置执行时间
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            // 设置ID
            task.setTaskId(taskinfo.getTaskId());

            // 保存任务日志
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(task, taskinfoLogs);
            // 乐观锁
            taskinfoLogs.setVersion(1);
            // 状态为初始化状态（添加）
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogs.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoLogsMapper.insert(taskinfoLogs);

            flag = true;
        } catch (BeansException e) {
            e.printStackTrace();
        }

        return flag;
    }
}