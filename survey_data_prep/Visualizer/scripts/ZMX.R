#Read/write a PB ZMX file
#
#Ben Stabler, stabler@pbworld.com, 080509
#Brian Gregor, Brian.J.GREGOR@odot.state.or.us, 052913
#Write updated to use 7zip - requires 7zip installed: http://www.7-zip.org
# Ben Stabler, ben.stabler@rsginc.com, 01/23/15
#Read updated to use gzcon and unz, which is much faster than before
# Ben Stabler, ben.stabler@rsginc.com, 01/22/16
#
#readZipMat("sovDistAm.zmx")
#writeZipMat(Matrix, "sovDistAm.zmx")

#Read ZMX File
readZipMat = function(fileName) {
  
  #define matrix
  rowCon = unz(fileName,"_rows")
  colCon = unz(fileName,"_columns")
  xRowNumCon = unz(fileName,"_external row numbers")
  xColNumCon = unz(fileName,"_external column numbers")
  nrows = as.integer(scan(rowCon, what="", quiet=T))
  ncols = as.integer(scan(colCon, what="", quiet=T))
  rowNames = strsplit(scan(xRowNumCon, what="", quiet=T),",")[[1]]
  colNames = strsplit(scan(xColNumCon, what="", quiet=T),",")[[1]]
  close(rowCon)
  close(colCon)
  close(xRowNumCon)
  close(xColNumCon)
  
  #create matrix
  outMat = matrix(0, nrows, ncols)
  rownames(outMat) = rowNames
  colnames(outMat) = colNames
  
  #read records
  zipEntries = paste("row_", 1:nrows, sep="")
  for(i in 1:nrows) {
    con = unz(fileName,zipEntries[i],"rb")
    outMat[i,] = readBin(con,what=double(),n=ncols, size=4, endian="big")
    close(con)
  }
  #return matrix
  return(outMat)
}


#Write ZMX File
writeZipMat = function(Matrix, FileName, SevenZipExe="C:/Program Files/7-Zip/7z.exe") {
  
  #Make a temporary directory to put unzipped files into
  tempDir = tempdir()
  print(tempDir)
  oldDir = getwd()
  setwd(tempDir)
  
  #Write matrix attributes
  cat(2, file="_version")
  cat(FileName, file="_name")
  cat(FileName, file="_description")
  
  cat(nrow(Matrix), file="_rows")
  cat(ncol(Matrix), file="_columns")
  cat(paste(rownames(Matrix),collapse=","), file="_external row numbers")
  cat(paste(colnames(Matrix),collapse=","), file="_external column numbers")
  
  #Write rows
  for(i in 1:nrow(Matrix)) {
    writeBin(Matrix[i,], paste("row_", i, sep=""), size=4, endian="big")
  }
  
  #Create file
  filesToInclude = normalizePath(dir(tempDir, full.names=T))
  filesToInclude = paste(paste('"', filesToInclude, '"\n', sep=""), collapse=" ")
  listFileName = paste(tempDir, "\\listfile.txt", sep="")
  write(filesToInclude, listFileName)
  setwd(oldDir)
  command = paste(paste('"', SevenZipExe, '"', sep=""), " a -tzip ", FileName, " @", listFileName, sep="")
  system(command)
}