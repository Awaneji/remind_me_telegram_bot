package com.tsuro.remind_me_bot.tutorial.configuration;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
@Slf4j
@AutoConfiguration
public class QuartzConfig {

    @Autowired
    ApplicationContext applicationContext;


    @Bean
    public JobFactory jobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/application.properties"));
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {

        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setQuartzProperties(quartzProperties());
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
        schedulerFactory.setAutoStartup(true);
        schedulerFactory.setJobFactory(jobFactory());
        schedulerFactory.setDataSource(quartzDataSource());
        return schedulerFactory;
    }

//    @Bean(name = "scheduler")
//    public Scheduler scheduler() throws IOException {
//        return schedulerFactoryBean().getScheduler();
//    }

//    public static SimpleTriggerFactoryBean createTrigger(JobDetail jobDetail, long pollFrequencyMs, String triggerName) {
//        log.debug("createTrigger(jobDetail={}, pollFrequencyMs={}, triggerName={})", jobDetail.toString(), pollFrequencyMs, triggerName);
//        JobDataMap dataMap = jobDetail.getJobDataMap();
//
//        Date startAt = (Date)dataMap.get("startAt");
//        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
//        factoryBean.setJobDetail(jobDetail);
//        factoryBean.setStartDelay(0L);
//        factoryBean.setRepeatInterval(pollFrequencyMs);
//        factoryBean.setName(triggerName);
//        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
//        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
//        factoryBean.setStartTime(startAt);
//
//
//        return factoryBean;
//    }
//
//    @Bean
//    public SchedulerFactoryBean scheduler(Trigger... triggers) throws IOException {
//        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
//
//        schedulerFactory.setQuartzProperties(quartzProperties());
//        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
//        schedulerFactory.setAutoStartup(true);
//        schedulerFactory.setJobFactory(jobFactory());
//        schedulerFactory.setOverwriteExistingJobs(true);
//        schedulerFactory.setDataSource(quartzDataSource());
//
//        if (ArrayUtils.isNotEmpty(triggers)) {
//            schedulerFactory.setTriggers(triggers);
//        }
//
//        return schedulerFactory;
//    }
//    public static JobDetailFactoryBean createJobDetail(Class jobClass, String jobName) {
//        log.debug("createJobDetail(jobClass={}, jobName={})", jobClass.getName(), jobName);
//
//        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
//        factoryBean.setName(jobName);
//        factoryBean.setJobClass(jobClass);
//        factoryBean.setDurability(true);
//
//
//        return factoryBean;
//    }

    @Bean
    @QuartzDataSource
    public DataSource quartzDataSource() {
        return DataSourceBuilder.create().build();
    }
}
