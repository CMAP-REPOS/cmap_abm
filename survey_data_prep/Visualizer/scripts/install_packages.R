install_if_missing = function(library) {
  if (!library %in% installed.packages()) {
    install.packages(library, repos='http://cran.us.r-project.org')
  }
}

libraries <- c("DT", "flexdashboard", "leaflet", "geojsonio", "htmltools", "htmlwidgets", "kableExtra", "shiny",
                        "knitr", "mapview", "plotly", "RColorBrewer", "rgdal", "rgeos", "crosstalk","treemap", "htmlTable",
                        "rmarkdown", "scales", "stringr", "jsonlite", "pander", "ggplot2", "reshape", "raster", "dplyr",
                        "chorddiag", "data.table", "tigris", "yaml", "sf", "plyr", "weights", "foreign", "reshape2")

suppressWarnings(suppressMessages(lapply(libraries, install_if_missing)))