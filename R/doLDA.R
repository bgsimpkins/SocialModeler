require(lda)

##Data for testing
# data(cora.documents)
# data(cora.vocab)

#####Params

##command line args
clArgs <- commandArgs(trailingOnly=T)

modelDir <- clArgs[1]

#Double quotes may be used to enclose. Remove.
modelDir <- gsub("\"","",modelDir)

K <- clArgs[2]
alpha <- clArgs[3]
eta <- clArgs[4]
nIter <- clArgs[5]
nBurnin <- clArgs[6]

docs <- read.documents(paste(modelDir,"docs.txt",sep=""))
vocab <- read.vocab(paste(modelDir,"features.txt",sep=""))



##Test command line values
# processId <- "test"  #CL arg (timestamp or unique id)
# docs <- cora.documents  #From file
# vocab <- cora.vocab     #From file
# K <- 10   #CL arg
# alpha <- 1  #CL arg
# eta <- .01  #CL arg
# nIter <- 300  #CL arg
# nBurnin <- 100  #CL arg

##End command line args

#####End params

#####Functions

proportions <- function(x)
{
  x/sum(x)
}

#####End functions

print("Fitting model...This may take a while...")
##Test model
fit <- lda.collapsed.gibbs.sampler(document=docs,K=K,vocab=vocab,alpha=alpha,eta=eta,num.iterations=nIter,burnin=nBurnin)

paste("Writing output to: ",getwd(),"/",modelDir,sep="")
##Get and write out feature 'probs'
word.prob <- apply(fit$topics,1,proportions)
write.csv(word.prob,paste(modelDir,"wordProbs",".csv",sep=""))

##Get and write out doc 'probs'
doc.prob <- apply(fit$document_sums,1,proportions)
write.csv(doc.prob,paste(modelDir,"docProbs",".csv",sep=""),row.names=FALSE)

##Get and write out topic 'probs'
topic.prob <- fit$topic_sums/sum(fit$topic_sums)
write.csv(topic.prob,paste(modelDir,"topicProbs",".csv",sep=""))
