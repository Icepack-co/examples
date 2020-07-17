rm(list =ls())
# test script
library(iceR)
library(gridExtra) # just for the plots at the end.

# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'nvd-hap0j2y4zlm1')
#api$endpoint <- 'localhost:8080/vehicle-router/solve/'

data <- read.csv('../sample_data/publist_orders.csv', stringsAsFactors = F)
ggplot() + geom_point(data = data, aes(x = X, y = Y)) + theme_bw()

set.seed(123)
data$Frequency <- sample(x = c(4,2,1), size = nrow(data),replace = T)
data$visitTime <- round(runif(n = nrow(data), 60, 120),0)
                      # assume that each visit takes somewhere between 1 and 2 hours.
                      # This would normally be a function of the input data.
                      # if you've ever been to an Irish pub, you'll know its tough to visit for less than an hour.

periodLength <- 20
weekLength <- 5
repHomeLocation <- 'My Sales Rep (@Guiness)'
data$id[1] <- repHomeLocation

m <- new (NVD.Model)
m$configuration <- new (NVD.Configuration)
m$configuration$timeUnit <- 1
m$configuration$timeCoef <- 1
m$configuration$timeCostCoef <- 0
m$configuration$distanceUnit <- 4
m$configuration$distanceCostCoef <- 1
m$configuration$intraTerritoryBalance <- 1
m$configuration$interTerritoryBalance <- 1
m$configuration$weekLength = weekLength
m$configuration$periodLength = periodLength
m$configuration$toString() %>% cat
t <- new(NVD.Territory)
    t$id <- repHomeLocation
    t$dailyStartTime <- 0
    t$dailyEndTime <- 400
    t$location <- new (NVD.Geocode)
    t$location$longitude <- data %>% filter(id == repHomeLocation) %>% select(X) %>% unlist()
    t$location$latitude <- data %>% filter(id == repHomeLocation) %>% select(Y) %>% unlist()
    t$dailyStartTime <- rep(0, periodLength)
    t$dailyEndTime <- rep(440, periodLength)  # assuming a 6h40m hour work day, we don't want them overwhelmed
                                                 # this can be modified quite easily on a day-to-day basis
m$territories <- t

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

  #   SEVEN_TIMES_A_WEEK = 1; #only applicable to a 7-day week
  #   SIX_TIMES_A_WEEK = 2;   #only applicable to a 6-day week
  #   FIVE_TIMES_A_WEEK = 3;
  #   FOUR_TIMES_A_WEEK = 4;
  #   THREE_TIMES_A_WEEK = 5;
  #   TWICE_A_WEEK = 6;       # it is unusual to implement this profile as we normally want twice a week with a certain gap between days.
  #   ONCE_A_WEEK = 7;        # frequency 4 in this example
  #   EVERY_SECOND_WEEK = 8;  # frequency 2 in this example
  #   ONCE_A_MONTH = 9;       # frequency 1 in this example
  if(s$Frequency == 4){
    v$profile$frequencyType =  7;
  }else if(s$Frequency == 2){
    v$profile$frequencyType = 8
  } else if (s$Frequency <= 1){
    v$profile$frequencyType = 9;
  }else {
    stop()
  }
  m$visits[[length(m$visits) + 1]]<- v
}

sr <- new (NVD.SolveRequest)
sr$model <- m
sr$solveType <- 0

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response. Takes around a minute to solve

tab <- tabulate(resp, sr) # you'll notice this tabulates the Pareto frontier here, since
                          # so solution from the frontier has been selected yet.
tab$frontier %>% head
resp %>% plotResponse(sr)

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

