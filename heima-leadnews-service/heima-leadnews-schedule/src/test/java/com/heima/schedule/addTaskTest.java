package com.heima.schedule;

import com.heima.common.exception.redis.CacheService;
import com.heima.model.common.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class addTaskTest {

    @Autowired
    private TaskService taskService;

    @Test
    public void test1(){

        Task task = new Task();
        task.setTaskType(100);
        task.setExecuteTime(new Date().getTime());
        task.setPriority(50);
        task.setParameters("nihao".getBytes(StandardCharsets.UTF_8));

        System.out.println(taskService.addTask(task));
    }
}
