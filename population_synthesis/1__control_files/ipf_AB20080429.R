#ipf.R
#Function to iteratively proportionally fit a multidimensional array
#IPF also known as Fratar method, Furness method, raking and two/three dimensional balancing.
#This method of matrix balancing is multiplicative since the margin factors (coefficients)
#are multiplied by the seed array to yield the balanced array. 
#Ben Stabler, benjamin.stabler@odot.state.or.us, 9.30.2003
#Brian Gregor, brian.j.gregor@odot.state.or.us, 2002

#inputs:
#1) marginsList=list(dim1=c(100,200),dim2=c(40,50,60))
# List of marginals corresponding to each dimension
#2) seedAry - a multi-dimensional array used as the seed for the IPF
#3) iteration counter (default to 100)
#4) closure criteria (default to 0.001)

#For more info on IPF see:
#Beckmann, R., Baggerly, K. and McKay, M. (1996). "Creating Synthetic Baseline Populations."
#   Transportation Research 30A(6), 415-435.
#Inro. (1996). "Algorithms". EMME/2 User's Manual. Section 6.
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
ipf <- function(marginsList, seedAry, maxiter=100, closure=0.001) {
    #Check to see if the sum of each margin is equal
    #if(length(unique(colSums(marginsMtx))) != 1) warning("sum of each margin not equal")

    #Replace margin values of zero with 0.001
    for(i in 1:length(marginsList)) {
		if(any(marginsList[[i]]==0)){
            marginsList[[i]][marginsList[[i]]==0] <- 0.001
        }
	}
	
	#Replace seed values of zero with 1
	seedAry <- ifelse(seedAry==0,1,seedAry)
	
    #Check to see if number of dimensions in seed array equals the number of
    #margins specified in the marginsMtx
    numMargins <- length(dim(seedAry))
    if(length(marginsList) != numMargins) {
        stop("number of margins in marginsMtx not equal to number of margins in seedAry")
    }

    #Set initial values
    resultAry <- seedAry
    iter <- 0
    marginChecks <- rep(1, numMargins)
    margins <- seq(1, numMargins)

    #Iteratively proportion margins until closure or iteration criteria are met
    while((any(marginChecks > closure)) & (iter < maxiter)) {
        for(margin in margins) {
            marginTotal <- apply(resultAry, margin, sum)
            marginCoeff <- marginsList[[margin]]/marginTotal
            resultAry <- sweep(resultAry, margin, marginCoeff, "*")
                marginChecks[margin] <- sum(abs(1 - marginCoeff))
        }    
        iter <- iter + 1
    }
    
    #If IPF stopped due to number of iterations then output info
    if(iter == maxiter) cat("IPF stopped due to number of iterations\n")

    #Return balanced array
    resultAry
}

