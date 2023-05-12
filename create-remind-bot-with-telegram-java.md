# Step-by-step guide to build a Telegram bot and interact with Java language  

## Telegram

Telegram is a cross-platform, encrypted instant messaging platform.  
The application offers two APIs for developers,  
* the Bot API that lets you create anc connect bots to the messaging platform and  
* the Telegram API and TDLib that allows developers build customised Telegram clients.  

Download the telegram application for mobile and desktop (Android, Apple, Windows, Linux, Mac-Os) from [Telegram](https://telegram.org/), create a user account and generate a token using the @BotFather bot.  
We use the token for integration with the Native language of choice, in our case Java.

### Creating your bot
Initiate a conversation with @BotFather within your Telegram account and follow the steps:

1. Press `start`.  
2. Type `/newbot`.  
3. Choose a name for your bot e.g. `Remind me`  
4. Choose a unique username for your bot (must end in "bot") e.g. `rangarira_bot`
5. Copy the generated token for integration e.g. `6087959083:AAGfRznu9Qrjbw_TD1EYhM7wHf6fN514gHf`

### Create commands in Telegram bot
We are going to use the @BotFather to create three commands `/start`, `/help` and `/bye`.  
Initiate a chat with @BotFather and follow instructions on command `/setCommands` to add your bot commands. We are going to interact with these commands in our application.

## Integration with java

Bootstrap a spring-boot application from the [Spring-boot initializr](https://start.spring.io/;) without adding any dependencies.  
Unzip the generating zip file and open the folder in your IDE of choice like Intellij.
Add the following dependency in the `pom.xml` file of the project you just opened:  

```xml
    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots</artifactId>
        <version>6.0.1</version>
    </dependency>
```
Below is the example project structure in Intellij:

```text
BotTutorial
├─ .idea
├─ src
│  └─ main
│     └─ java
│        └─ tutorial
│           └─ TelegramBotApplication
└─ pom.xml
```

Create your `Bot` class which extends from `TelegramLongPollingBot`, a class from the Telegram dependency added above:

```java
public class Bot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        // this is your bot username
        return "rangarira_bot";
    }

    @Override
    public String getBotToken() {
        // this is your generated token with @BotFather
        return "6087959083:AAGfRznu9Qrjbw_TD1EYhM7wHf5fN514gHf";
    }

    @Override
    public void onUpdateReceived(Update update) {

    }
}
```

Within this `Bot` class we are going to add all the logic for Remind me in the override methods:  
* getBotUsername - returns your bot username (set using @BotFather)
* getBotToken - returns your bot token (set using @BotFather)
* onUpdateReceived - handles messages from interacting with the bot in Telegram

In the `@SpringBootApplication` class, we initialize the `Bot` class to start the Telegram bot session as follows:  
```java
@SpringBootApplication
public class TelegramBotApplication {
	public static void main(String[] args) throws TelegramApiException {
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
		telegramBotsApi.registerBot(new Bot());
		SpringApplication.run(TelegramBotApplication.class, args);
	}
}
```
Running the main method will create an interface with our bot identified by the token and username above listening for our Remind-me messages.  

### onUpdateReceived method
Add the following code in the onUpdateReceived method to receive user interactions with the Telegram bot:  
```java 
    @Override
    public void onUpdateReceived(Update update) {

        var msg = update.getMessage();
        var user = msg.getFrom();
        List<ModelResult> modelResultList = DateTimeRecognizer.recognizeDateTime(msg.getText(), Culture.English, DateTimeOptions.SplitDateAndTime);
        if (msg.isCommand()) {
            if (msg.getText().equals("/help"))
                listCommands(user.getId());
            else if (msg.getText().equals("/bye"))
                terminateSession(user);
            else if (msg.getText().equals("/start")) {
                sendText(user.getId(), "Welcome " + user.getFirstName() + " to the rangarira/ remind me Bot, you can now schedule your queries!!");
            } else if (modelResultList.size() > 0) {
                scheduleQuery(update);
            } else {
                listCommands(user.getId());
            }
        } else if (modelResultList.size() > 0) {
            scheduleQuery(update);
        } else {
            listCommands(user.getId());
        }
    }
```
The snippet above receives an `Update update` object which has a Message object accessed using `getMessage()` method where we are extracting the message text and the user id for use on interaction with the bot.  

#### Code breakdown
The line with `msg.isCommand()` is used to check if the update received has either the commands (`/start, /help or /bye`):
* /start - invokes the below method `sendText()`, which sends a Welcome message to the user 
```java
    private void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
```
* /help - invokes the below method `listCommands()`, which list our bot commands
```java
    private void listCommands(Long id) {
        String message = """
                Hello!, Kindly type any of the commands below
                 /start to start interaction
                 /help to get help
                 /bye to exit interaction,
                 ==============================================
                 you can now schedule your queries using text!!
                 ==============================================""";
        sendText(id, message);
    }
```
* /bye - invokes the below method `terminateSession()`, which prints a message for a session termination
```java
    private void terminateSession(User user) {
        sendText(user.getId(), "Good bye! " + user.getFirstName());
    }
```
Otherwise, the message received, if it is not one of the above commands, it is assumed to be a query to schedule. The below method `scheduleQuery(Update update)`, receives the queries, decodes the text in the message to a Date or Time value and Schedule the message via a `Scheduler`.  
For us to be able to process the user entered text we need the following `pom.xml` dependencies from [Microsoft Recognizers Text](https://github.com/microsoft/Recognizers-Text/):
```xml
<dependency>
    <groupId>com.microsoft.recognizers.text.datetime</groupId>
    <artifactId>recognizers-text-date-time</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>com.microsoft.recognizers.text</groupId>
    <artifactId>recognizers-text</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
These dependencies allow us to process text as shown in the below table:

| user text                  | decoded text                                       |
|----------------------------|----------------------------------------------------|
| go to church, noon         | GO TO CHURCH AT 12:00:00                           |
| joyful party next Monday   | JOYFUL PARTY ON 2023-04-24                         |
| go to church this Friday   | GO TO CHURCH  ON 2023-04-21                        |
| visit family, this Sunday  | VISIT FAMILY,  ON 2023-04-23                       |
| I'll go back 8pm today     | I'LL GO BACK  ON 2023-04-20 20:00:00               |
| Fly to Dubai, in five days | FLY TO DUBAI, IN  ON 2023-04-25T14:15:23.040487300 |
|                            |                                                    |

The code is below:  
```java
private void scheduleQuery(Update update) {
        var msg = update.getMessage();
        var userId = msg.getFrom().getId();
        try {
            List<ModelResult> modelResultList = DateTimeRecognizer.recognizeDateTime(msg.getText(), Culture.English, DateTimeOptions.SplitDateAndTime);
    
            if (modelResultList.size() > 0) {
            ModelResult result = modelResultList.get(0);
            SortedMap<String, Object> sortedMap = result.resolution;
    
            @SuppressWarnings("unchecked") ArrayList<Object> objects = (ArrayList<Object>) sortedMap.get(sortedMap.lastKey());
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(objects.get(objects.size() - 1));
            DecodedMessage decodedMessage = objectMapper.readValue(json, DecodedMessage.class);
    
            String joiner = decodedMessage.getType().equals("time") ? " at " : " on ";
            var resp = "";
            var reminderTime = "";
            LocalDateTime scheduleTime;
            if (decodedMessage.getType().equals("duration")) {
                scheduleTime = LocalDateTime.now().plusSeconds(Long.parseLong(decodedMessage.getValue()));
                joiner = " on ";
                resp = msg.getText().toUpperCase().replace(result.text.toUpperCase(), joiner.concat(scheduleTime.toString()).toUpperCase());
            } else {
                resp = msg.getText().toUpperCase().replace(result.text.toUpperCase(), joiner.concat(decodedMessage.getValue()).toUpperCase());
                reminderTime = decodedMessage.getValue();
        
                try {
                    scheduleTime = LocalDateTime.parse(reminderTime, DateTimeFormatter.ISO_DATE);
                } catch (Exception e) {
        
                // time object
                String[] timeValues = reminderTime.split(":");
                scheduleTime = LocalDateTime.of(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth(), LocalDateTime.now().getDayOfMonth(), Integer.parseInt(timeValues[0]), Integer.parseInt(timeValues[1]), Integer.parseInt(timeValues[2]));
        
                }
            }
    
            invokeQuartzScheduling(msg, userId, scheduleTime);
    
            // send message back to bot
            sendText(userId, "SCHEDULED " + resp);
            } else {
                sendText(userId, "An error occurred decoding your query, kindly rephrase.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            sendText(userId, "An error occurred decoding your query, kindly rephrase.");
        }
}
```
In the snippet above, the line below, is used to recognize and decode the user message (`msg.getText()`) sent to the bot for processing and returns a list of objects of type `ModelResult` which we traverse to get to the processed data which has a `type`, `value`, and `timex` or more fields depending on scenario.  
* `type` - values can be a `date`, `daterange`, `duration`, `time` e.t.c.
* `value` - the parsed value
* `timex` - the timex string type values include [P5D, PW4, P1M, T12] and so on.
```java
    List<ModelResult> modelResultList = DateTimeRecognizer.recognizeDateTime(msg.getText(), Culture.English, DateTimeOptions.SplitDateAndTime);
```


### Scheduling
For the bot to send back a message on the time required by the user, a CronJob or a Scheduler can be used in Spring with a library like Quartz.
The Quartz Scheduler provides multiple classes for scheduling Jobs given the time we extract from the user input, the date or time and the actual message which we can relay back to the user at the scheduled time.  
To track the scheduled jobs, we are going to add a mysql, jpa and quartz dependencies listed below:  
```xml
        <dependency>
			<groupId>jakarta.persistence</groupId>
			<artifactId>jakarta.persistence-api</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-context-support</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>

		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>
```

Also, we need to update the application.properties file as below, to cater for mysql and quartz properties:  
```properties

    #mysql database connection
    spring.datasource.url=jdbc:mysql://localhost:3306/remind_me
    #spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
    spring.datasource.username=root
    spring.datasource.password=migs
    spring.datasource.timeBetweenEvictionRunsMillis=60000
    spring.datasource.maxIdle=1
    spring.jpa.generate-ddl=true
    spring.jpa.hibernate.ddl-auto=update
    
    #Quartz Log level
    logging.level.org.springframework.scheduling.quartz=DEBUG
    logging.level.org.quartz=DEBUG
    
    
    #Quartz
    org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
    org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
    org.quartz.jobStore.dataSource=quartzDataSource
    spring.quartz.job-store-type=jdbc
    org.quartz.jobStore.tablePrefix=QRTZ_
```

### Quartz implementation
We will add quartz configuration and set up `beans` which will interact with the bot for sending and receiving jobs on schedule. The Quartz configuration is outlined as follows:  
```java

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

    @Bean
    @QuartzDataSource
    public DataSource quartzDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```
The Spring's `SchedulerFactoryBean` is a `FactoryBean` for creating and configuring a Quartz Scheduler. It also manages the scheduler's life cycle and exposes the scheduler as bean reference. The `QuartzConfig` creates a bean reference of `SchedulerFactoryBean` and `JobFactory`.  
A JobFactory is responsible for producing Job instances. This class should be annotated with @Configuration annotation. The @Configuration annotation indicates that this is a configuration class and will be used by the Spring application context to create beans for your application.  
To make autowiring from inside the Job class possible, we need to create a Java class that should extend `SpringBeanJobFactory` and implement `ApplicationContextAware`, we will create a `AutowiringSpringBeanJobFactory` class to bring Spring and Quartz together.

```java
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory
implements ApplicationContextAware{

    AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        beanFactory = context.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        final Object job = super.createJobInstance(bundle);
        beanFactory.autowireBean(job);
        return job;
    }
}
```
### The Entity class
The application saves messages data in a MySQL database table and updates the reminder object's Active value to false at the scheduled time.  
To perform these operations we will create an entity class `Reminder` class.  
```java
@Entity
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reminder_time")
    private LocalDateTime reminderTime;

    @Column(name = "reminder_message")
    private String reminderMessage;

    private boolean active;

    @CreatedDate
    private LocalDateTime created;
    @LastModifiedDate
    private LocalDateTime modified;

    @Column(name = "telegram_user")
    private Long telegramUserId;
}
```

### The Repository interface
To perform CRUD (Create Read Update Delete) operations on the table, we create an interface and extend it with{" "} `JpaRepository` interface.  
The `JpaRepository` interface provides generic CRUD operations on a repository for a specific type.
```java
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findAllByTelegramUserId(String userId);
}
```

### The Job class
Job class
A Job can be any Java class that implements the Job interface of Quartz, we will create a `ReminderJob` class.  
The Job interface has a single execute method where the actual work for that particular job is written.   
Inside the execute method, you can retrieve data that you may wish an instance of that job instance must have while it executes.  
The data are passed to a Job instance via the `JobDataMap` class which is part of the `JobDetail` object. The data are stored in `JobDataMap` prior to adding the job to the scheduler.
We will retrieve the Reminder information here and update the `Active` field before sending back an alarm message to the user via `Bot` `sendText()` method.
```java
@Component
public class RemindersJob implements Job {


    private final ReminderService reminderService;

    private final Bot bot;

    @Autowired
    public RemindersJob(ReminderService reminderService, Bot bot) {

        this.reminderService = reminderService;
        this.bot = bot;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        /* Get message id recorded by scheduler during scheduling */
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String messageId = dataMap.getString("messageId");

//        log.info("Executing job for message id {}", messageId);

        /* Get message from database by id */
        long id = Long.parseLong(messageId);
        Reminder reminder = reminderService.retrieveReminder(id);

        reminder.setActive(false);
        reminderService.updateReminder(id, reminder);

        bot.sendText(reminder.getTelegramUserId(),"********* ALARM FOR ********* : "+reminder.getReminderMessage());
        System.out.println("Job trigger at " + LocalDateTime.now() + " and was scheduled to run at " + reminder.getReminderTime());

        /* unschedule or delete after job gets executed */

        try {
            context.getScheduler().deleteJob(new JobKey(messageId));

            TriggerKey triggerKey = new TriggerKey(messageId);

            context.getScheduler().unscheduleJob(triggerKey);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
```

### Interaction with the Scheduler
In the Bot class, we will invoke the `invokeQuartzScheduling()` method which will handle creation of a `Reminder` object after decoding the message received from the user when scheduling an event/task.  
Here we will pass the Job specifics including the job time, and job id, we will persist the `Reminder` object to the database in this instance which be retrieved when the `ReminderJob` executes.  
```java
private void invokeQuartzScheduling(Message msg, Long userId, LocalDateTime scheduleTime) throws SchedulerException {
        // create reminder object
        Reminder reminder = new Reminder();
        reminder.setActive(true);
        reminder.setCreated(LocalDateTime.now());
        reminder.setReminderTime(scheduleTime);
        reminder.setModified(LocalDateTime.now());
        reminder.setTelegramUserId(userId);
        reminder.setReminderMessage(msg.getText());

        reminder = reminderService.createReminder(reminder);

        // Creating JobDetail instance
        String id = String.valueOf(reminder.getId());
        JobDetail jobDetail = JobBuilder.newJob(RemindersJob.class).withIdentity(id).build();

        // Adding JobDataMap to jobDetail
        jobDetail.getJobDataMap().put("messageId", id);
        Date triggerJobAt = Date.from(reminder.getReminderTime().toInstant(ZoneOffset.ofHours(2)));
        jobDetail.getJobDataMap().put("startAt", triggerJobAt);

        SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity(id)
                .startAt(triggerJobAt).withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .build();

        // Getting scheduler instance
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
    }
```

## Bot initialization in Spring context
We will initialize the Bot in a separate `BotInitializer` class to allow for proper use of beans in the Context.

```java
@Slf4j
@Component
public class BotInitializer {
    private final Bot tgBot;

    @Autowired
    public BotInitializer(Bot tgBot) {
        this.tgBot = tgBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try{
            telegramBotsApi.registerBot(tgBot);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
```

## Cases not covered
The Remind-me bot is not able to process some test cases as below:
* `Fly to Dubai, start of next month` which false under the `daterange` type will throw an exception on our bot
* `joyful party this Monday`, will not retrieve a future Monday but the immediate past Monday
* time specific schedules may throw exceptions because they won't return a date part when decode for example `go to church at noon`


## Project repository  
[Project git repo](https://github.com/Awaneji/remind_me_telegram_bot.git)


## Further reading
[Recognizers-Text-Java](https://github.com/microsoft/Recognizers-Text/tree/master/Java)  
[Quartz Scheduler](https://www.baeldung.com/spring-quartz-schedule) 
[Dzone Quartz Tutorial](https://dzone.com/articles/adding-quartz-to-spring-boot)




