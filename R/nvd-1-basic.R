# NVD EXAMPLE SCRIPT (BASIC)

library(iceR)
library(gridExtra)

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'nvd-hap0j2y4zlm1', configFile = '../config.json')

# Create the model
sr <- new (NVD.SolveRequest)
sr$model <- new (NVD.Model)


# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv', stringsAsFactors = F)

# View the loaded data (if desired)
ggplot() + geom_point(data = data, aes(x = X, y = Y)) + theme_bw()

set.seed(123)
data$Frequency <- sample(x = c(4,2,1), size = nrow(data),replace = T)
data$visitTime <- round(runif(n = nrow(data), 60, 120),0)
                    
repHomeLocation <- 'My Sales Rep (@Guinness)'
data$id[1] <- repHomeLocation

periodLength <- 20
weekLength <- 5

sr$model$configuration <- new (NVD.Configuration)
sr$model$configuration$timeUnit <- 1
sr$model$configuration$timeCoef <- 1
sr$model$configuration$timeCostCoef <- 0
sr$model$configuration$distanceUnit <- 4
sr$model$configuration$distanceCostCoef <- 1
sr$model$configuration$intraTerritoryBalance <- 1
sr$model$configuration$interTerritoryBalance <- 1
sr$model$configuration$weekLength = weekLength
sr$model$configuration$periodLength = periodLength
sr$model$configuration$toString() %>% cat

t <- new(NVD.Territory)
t$id <- repHomeLocation
t$dailyStartTime <- 0
t$dailyEndTime <- 400
t$location <- new (NVD.Geocode)
t$location$longitude <- data %>% filter(id == repHomeLocation) %>% select(X) %>% unlist()
t$location$latitude <- data %>% filter(id == repHomeLocation) %>% select(Y) %>% unlist()
t$dailyStartTime <- rep(0, periodLength)
t$dailyEndTime <- rep(440, periodLength)
sr$model$territories <- t

for(i in 2:nrow(data)){
  v <- new (NVD.Visit)
  s<- data[i,]
  v$id <- as.character(s$id)
  v$location <- new(NVD.Geocode)
  v$location$longitude <- s$X
  v$location$latitude <- s$Y
  v$visitTime <- s$visitTime
  v$territoryRelations <- new (NVD.Visit.TerritoryRelation)
  v$territoryRelations$type <- 0 # 0 INCLUSIVE, 1 EXCLUSIVE
  v$territoryRelations$territoryIds <- repHomeLocation

  if(s$Frequency == 4){
    v$profile$frequencyType =  7;
  }else if(s$Frequency == 2){
    v$profile$frequencyType = 8
  } else if (s$Frequency <= 1){
    v$profile$frequencyType = 9;
  }else {
    stop()
  }
  sr$model$visits[[length(sr$model$visits) + 1]]<- v
}

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

# View the model in plain-text format (string or JSON)
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)     
tab <- tabulate(resp, sr) 
tab$frontier %>% head

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)








# this is the cheapest solution
instanceCheap <-  api %>% getSolutionInstance(sr, reps, solutionIndex = 1)

instanceCheap %>% plotResponse(sr)

# this is the most balanced solution
instanceBalanced <- api %>% getSolutionInstance(sr, reps, solutionIndex = length(resp$frontier))
instanceBalanced %>% plotResponse(sr)

l<- tabulate(instanceCheap, sr)
l$nodes %>% head
l$edges$distance_cost %>% sum

l$nodes %>%
  filter(!grepl(pattern = repHomeLocation, taskId, fixed =T)) %>%
  group_by(taskId) %>% summarise(numVisits = n(), visitDays = paste0(day, collapse = ','))
# so this is a nice way to understand NVD models. Frequency 2 must be exactly 10 days apart for each visit,
# Frequency 4 must be 5 days between each visit etc. This is what makes these routing problems hard.

# instanceCheap %>% plotResponseLeaflet(sr)
# the leaflet repsonse is less interesting because we don't split it by day like we do in ggplot

summarise_activity<- function(l){
  e <- l$edges
  e$geometry <- NULL
  tmp1<- e %>% group_by(day) %>% summarise(travel_time_mins = sum(time_end - time_start), distance_km = sum(distance_end- distance_start))
  tmp2<- l$nodes %>% group_by(day) %>% summarise(visit_time_mins = sum(time_end - time_start))
  df<- data.frame(day = unique(c(l$edges$day,l$nodes$day)))
  df %<>% left_join(tmp1, by = 'day') %>% left_join(tmp2, by = 'day')
  df %<>% mutate(total_time_mins = travel_time_mins + visit_time_mins)
  return(df)
}

p1 <- ggplot() +
  geom_bar(data = summarise_activity(instanceCheap %>% tabulate(sr)),
           aes(x = day, y = total_time_mins), stat = 'identity') + theme_bw() + ylab("Total Time (Minutes)")
p2 <- instanceCheap %>% plotResponse(sr)
p3 <- ggplot() +
  geom_bar(data = summarise_activity(instanceBalanced %>% tabulate(sr)),
           aes(x = day, y = total_time_mins), stat = 'identity') + theme_bw() + ylab("Total Time (Minutes)")
p4 <- instanceBalanced %>% plotResponse(sr)

grid.arrange(p1, p2, nrow = 1)
grid.arrange(p3, p4, nrow = 1)

l<- rbind(summarise_activity(instanceCheap %>% tabulate(sr)) %>% mutate(solution = 'cheap'),
      summarise_activity(instanceBalanced %>% tabulate(sr)) %>% mutate(solution = 'balanced'))
ggplot(data = l) +
  geom_bar( aes(x = day, y = total_time_mins), stat = 'identity') +
  facet_grid(solution ~.) + ylab("Total Time (Minutes)") + theme_bw()

#illustrates how you have many empty days as well as higher peaks (and generally a larger range)

l %>%
  group_by(solution) %>%
  summarise(total_minutes = sum(total_time_mins),
            total_distance = sum(distance_km),
            daily_variance = var(total_time_mins))
# illustrates that the one has less workload (time and distance) but is less balanced.

