
cross_tab = function(
  data,
  variable_labels,
  value_labels,
  variables,
  tab_by = NULL,
  missing_values = NULL,
  row_totals = FALSE,
  rounding_precision = 1,
  weight_column = NULL
){
  
  if (row_totals == TRUE & is.null(tab_by)) {
    stop("Error: parameters row_totals and tab_by are inconsistent")
  }
  #browser()
  table = copy(data)
  
  if (is.null(weight_column)) {
    table[, weight := 1]
  } else {
    setnames(table, weight_column, "weight")
  }
  
  cross_tab_table =
    table[,
          c(variables, tab_by, "weight"),
          with = FALSE]
  
  cross_tab_table =
    suppressWarnings(
      melt(
        cross_tab_table,
        id.vars = c(tab_by, "weight")))
  
  cross_tab_table[, value := as.character(value)]
  value_labels[, value := as.character(value)]
  
  cross_tab_table = value_labels[cross_tab_table, on = .(variable, value)]
  
  cross_tab_table[is.na(value_label) & !is.na(value), value_label := paste0("Missing label (value = ", value, ")")]
  
  cross_tab_table[value %in% missing_values, valid := FALSE]
  cross_tab_table[is.na(valid), valid := TRUE]
  
  cross_tab_table =
    rollup(
      cross_tab_table,
      j = sum(weight),
      c(tab_by, "valid", "variable", "value_label", "value"))
  
  setnames(cross_tab_table, "V1", "N")
  
  cross_tab_table = cross_tab_table[!(is.na(variable) & !is.na(valid))]
  
  cross_tab_table = cross_tab_table[!(is.na(value) & valid == FALSE)]
  
  if (row_totals == TRUE) {
    #browser()
    cross_tab_table =
      rbindlist(
        list(
          cross_tab_table,
          cross_tab_table[, .(N = sum(N)), .(value_label, valid, variable, value)]),
        use.names = TRUE,
        fill = TRUE)
    
    # things are alphabetical. This forces totals to the right. will rename
    # after casting
    cross_tab_table[is.na(get(tab_by)) & !is.na(variable), (tab_by) := 99999999]
    
  }
  
  cross_tab_table[
    valid == TRUE & !is.na(value),
    pct_valid := paste0(format(round(N / sum(N), rounding_precision + 2) * 100, nsmall = rounding_precision), "%"),
    by = c(tab_by, 'variable')]
  
  cross_tab_table = cross_tab_table[!(is.na(value) & !is.na(value_label))]
  
  if (!is.null(tab_by)) {
    
    cross_tab_table = cross_tab_table[!is.na(get(tab_by))]
    
  }
  
  cross_tab_table[valid == TRUE & is.na(value), pct_valid := paste0(format(100, nsmall = rounding_precision), "%")]
  
  totals = cross_tab_table[is.na(value), .(N = sum(N)), c(tab_by, 'variable')]
  setnames(totals, "tab_by", ifelse(is.null(tab_by), '', tab_by), skip_absent = TRUE)
  
  totals[, value_label := "Total"]
  totals[, value := Inf]
  
  cross_tab_table = rbindlist(list(cross_tab_table, totals), use.names = TRUE, fill = TRUE)
  
  if (!is.null(tab_by)) {
    cross_tab_table[order(tab_by)]
    if (tab_by %in% value_labels[, variable]) {
      
      cross_tab_table[, (tab_by) := as.character(get(tab_by))]
      
      cross_tab_table =
        value_labels[
          variable == tab_by,
          .(tab_by_value = value,
            tab_by_value_label = paste0(value, ' - ', value_label))] %>%
        .[cross_tab_table, on = c("tab_by_value" = tab_by)]
      
      cross_tab_table[tab_by_value == 99999999, tab_by_value_label := "zzzTotal"]
      
      cross_tab_table[, tab_by_value := NULL]
      
      setnames(cross_tab_table, "tab_by_value_label", tab_by)
      
    }
    
    cross_tab_table =
      dcast(
        cross_tab_table,
        variable + value + value_label + valid ~ get(tab_by),
        value.var = c("N", "pct_valid"),
        fun.aggregate = function(x){x[1]})
  }
  
  cross_tab_table =
    variable_labels[, .(variable, description, logic)] %>%
    .[cross_tab_table, on = .(variable)]
  
  for (name in names(cross_tab_table)[grepl("N_", names(cross_tab_table))]) {
    cross_tab_table[, (name) := format(round(get(name), 0), big.mark = ",", scientific = FALSE)]
    cross_tab_table[grepl("NA", get(name)), (name) := '']
  }
  
  names(cross_tab_table) = gsub("N_", "", names(cross_tab_table))
  names(cross_tab_table) = gsub("pct_valid_", "", names(cross_tab_table))
  names(cross_tab_table) = gsub("zzzT", "T", names(cross_tab_table))
  
  cross_tab_table[is.na(value_label) & valid == TRUE, value_label := "Valid total"]
  cross_tab_table[is.na(value_label) & valid == FALSE, value_label := "Missing total"]
  
  cross_tab_table = cross_tab_table[!is.na(variable)]
  
  cross_tab_table = suppressWarnings(cross_tab_table[order(variable, -valid, as.numeric(value))])
  
  return(cross_tab_table)
  
}


format_table = function(tab,footnote_text=NA){
  
  tab = tab %>%
    kable(format='html', row.names=FALSE,  format.args = list(big.mark = ",")) %>%
    kable_styling(full_width=FALSE, position='left',
                  bootstrap_options=c('striped', 'condensed', 'bordered', 'hover'))
  
  if(!is.na(footnote_text)){
    tab = tab %>% footnote(general = footnote_text)
  }
  
  return(tab)
  
}


format_crosstab_table =
  function(
    table_to_format
  ) {
    variable = unique(table_to_format[, variable])
    description = unique(table_to_format[, description])
    logic = unique(table_to_format[, logic])
    
    table_to_format[, variable := NULL]
    table_to_format[, value := NULL]
    table_to_format[, valid := NULL]
    
    setnames(table_to_format, "value_label", variable)
    
    cat(paste0("#### ", description, "\n"))
    
    if (!is.na(logic)) {
      cat(paste0(logic, "\n"))
    }
    
    table_to_format[, description := NULL]
    table_to_format[, logic := NULL]
    
    column_width = (ncol(table_to_format) - 1) / 2
    
    align_string = paste0("l", paste0(rep("r", column_width * 2), collapse = ""))
    
    table_to_format[is.na(table_to_format)] = ""
    
    table_to_format =
      table_to_format %>%
      kable(align = align_string) %>%
      kable_styling(bootstrap_options = c("striped", "hover", "condensed", "responsive"), font_size = 13) %>%
      add_header_above(c(" " = 1, "Counts" = column_width, "Percentages" = column_width)) %>%
      column_spec(1, width = "30em", border_right = "1px solid lightgray") %>%
      column_spec(1 + column_width, border_right = "1px solid lightgray") %>%
      column_spec(1 + 2 * column_width, border_right = "1px solid lightgray")
    
    print(table_to_format)
    
  }
