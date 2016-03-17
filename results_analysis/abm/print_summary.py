#!/usr/bin/env python
'''
    print_summary.py
    Author: npeterson
    Revised: 3/16/16
    ---------------------------------------------------------------------------
    A set of functions for printing summaries of ABM and Comparison objects
    to the terminal.

'''
import abm
import comparison

# -----------------------------------------------------------------------------
#  Define functions for printing ABM summaries to terminal.
# -----------------------------------------------------------------------------
def mode_share(abm, grouped=False):
    ''' Print the mode share of trips. '''
    print ' '
    if grouped:
        mode_share_grouped = {
            'Auto (Excl. Taxi)': sum(abm.mode_share[m] for m in xrange(1, 7)),
            'Drive-to-Transit': sum(abm.mode_share[m] for m in (11, 12)),
            'Walk-to-Transit': sum(abm.mode_share[m] for m in (9, 10)),
            'Walk/Bike/Taxi/School Bus': sum(abm.mode_share[m] for m in (7, 8, 13, 14))
        }
        print 'MODE SHARE (GROUPED)'
        print '--------------------'
        for mode in sorted(mode_share_grouped.keys()):
            print '{0:<25}{1:>10.2%}'.format(mode, mode_share_grouped[mode])
    else:
        print 'MODE SHARE'
        print '----------'
        for mode in sorted(abm.modes.keys()):
            print '{0:<25}{1:>10.2%}'.format(abm._get_mode_str(mode), abm.mode_share[mode])
    print ' '
    return None


def ptrips_by_class(abm):
    ''' Print the number and percentage of transit person-trips, stratified
        by user class. '''
    print ' '
    print 'LINKED TRANSIT PERSON-TRIPS BY USER CLASS'
    print '-----------------------------------------'
    total_ptrips = sum(abm.ptrips_by_class.itervalues())
    for uclass in sorted(abm.ptrips_by_class.keys()):
        ptrips = abm.ptrips_by_class[uclass]
        ptrips_pct = ptrips / total_ptrips
        print '{0:<25}{1:>10,.0f} ({2:.2%})'.format('User Class {0}'.format(uclass), ptrips, ptrips_pct)
    print '{0:<25}{1:>10,.0f}'.format('All User Classes', total_ptrips)
    print ' '
    return None


def transit_stats(abm, grouped=False):
    ''' Print the boardings, passenger miles traveled and passenger hours
        traveled, by mode or grouped. '''
    print ' '
    if grouped:
        total_boardings = sum(abm.transit_stats['BOARDINGS'].itervalues())
        total_pmt = sum(abm.transit_stats['PMT'].itervalues())
        total_pht = sum(abm.transit_stats['PHT'].itervalues())
        print 'TRANSIT STATS'
        print '-------------'
        print ' {0:<15} | {1:<15} | {2:<15} '.format('Boardings', 'Pass. Miles', 'Pass. Hours')
        print '{0:-<17}|{0:-<17}|{0:-<17}'.format('')
        print ' {0:>15,.0f} | {1:>15,.0f} | {2:>15,.0f} '.format(total_boardings, total_pmt, total_pht)
    else:
        print 'TRANSIT STATS BY MODE'
        print '---------------------'
        print ' {0:<20} | {1:<15} | {2:<15} | {3:<15} '.format('Mode', 'Boardings', 'Pass. Miles', 'Pass. Hours')
        print '{0:-<22}|{0:-<17}|{0:-<17}|{0:-<17}'.format('')
        for mode_code, mode_desc in sorted(abm.transit_modes.iteritems(), key=lambda (k, v): v):
            boardings = abm.transit_stats['BOARDINGS'][mode_code]
            pmt = abm.transit_stats['PMT'][mode_code]
            pht = abm.transit_stats['PHT'][mode_code]
            print ' {0:<20} | {1:>15,.0f} | {2:>15,.0f} | {3:>15,.0f} '.format(mode_desc, boardings, pmt, pht)
    print ' '
    return None


def vmt_by_speed(abm):
    ''' Print the total daily VMT by 5-mph speed bin, including a crude
        histogram. '''
    print ' '
    print 'DAILY VMT BY SPEED (MPH)'
    print '------------------------'
    total_vmt = sum(abm.vmt_by_speed.itervalues())
    for speed_bin in sorted(abm.vmt_by_speed.keys()):
        vmt = abm.vmt_by_speed[speed_bin]
        vmt_pct = vmt / total_vmt
        if speed_bin <= 65:
            bin_label = '{0}-{1}'.format(speed_bin, speed_bin+5)
        else:
            bin_label = '70+'
        numbers = '{0:<6}{1:>15,.2f} ({2:.2%})'.format(bin_label, vmt, vmt_pct)
        print '{0:<30}  {1}'.format(numbers, '|' * int(round(vmt_pct * 100)))
    print '{0:<6}{1:>15,.2f}'.format('Total', total_vmt)
    print ' '
    return None


# -----------------------------------------------------------------------------
#  Define functions for printing Comparison summaries to terminal.
# -----------------------------------------------------------------------------
def mode_share_change(comparison, grouped=True):
    ''' Print the change in mode share, grouped into broader categories
        (default), or ungrouped. '''
    mode_share_diff = {}
    for mode in sorted(abm.ABM.modes.keys()):
        mode_share_diff[mode] = comparison.test.mode_share[mode] - comparison.base.mode_share[mode]
    print ' '
    if grouped:
        mode_share_grouped_diff = {
            'Auto (Excl. Taxi)': sum(mode_share_diff[m] for m in xrange(1, 7)),
            'Drive-to-Transit': sum(mode_share_diff[m] for m in (11, 12)),
            'Walk-to-Transit': sum(mode_share_diff[m] for m in (9, 10)),
            'Walk/Bike/Taxi/School Bus': sum(mode_share_diff[m] for m in (7, 8, 13, 14))
        }
        print 'MODE SHARE CHANGE (GROUPED)'
        print '---------------------------'
        for mode in sorted(mode_share_grouped_diff.keys()):
            print '{0:<25}{1:>+10.2%}'.format(mode, mode_share_grouped_diff[mode])
    else:
        print 'MODE SHARE CHANGE'
        print '-----------------'
        for mode in sorted(mode_share_diff.keys()):
            print '{0:<25}{1:>+10.2%}'.format(comparison.base._get_mode_str(mode), mode_share_diff[mode])
    print ' '
    return None


def new_trips_for_mode(comparison, mode_list, mode_description, table='Trips'):
    ''' Identify the increase (or decrease) in trips/tours for a given set
        of modes. '''
    comparison.open_dbs()
    sql_where = ' OR '.join(('mode={0}'.format(mode) for mode in mode_list))
    base_trips = comparison.base._unsample(comparison.base._count_rows(table, sql_where))
    test_trips = comparison.test._unsample(comparison.test._count_rows(table, sql_where))
    comparison.close_dbs()
    new_trips = test_trips - base_trips
    pct_new_trips = new_trips / base_trips
    print ' '
    print mode_description.upper()
    print '-' * len(mode_description)
    print '{0:<25}{1:>10,.0f}'.format('Base daily {0}'.format(table.lower()), base_trips)
    print '{0:<25}{1:>10,.0f}'.format('Test daily {0}'.format(table.lower()), test_trips)
    print '{0:<25}{1:>+10,.0f} ({2:+.2%})'.format('Daily {0} change'.format(table.lower()), new_trips, pct_new_trips)
    print ' '
    return None

# Wrapper methods for print_new_trips_for_mode():
def new_trips_all(comparison, table='Trips'):
    ''' All-trips wrapper for print_new_trips_for_mode(). '''
    return new_trips_for_mode(comparison, xrange(1, 15), 'Change in Total {0}'.format(table), table)
def new_trips_auto(comparison, table='Trips'):
    ''' Auto-trips wrapper for print_new_trips_for_mode(). '''
    return new_trips_for_mode(comparison, xrange(1, 7), 'Change in Auto {0}'.format(table), table)
def new_trips_dtt(comparison, table='Trips'):
    ''' Drive-to-transit wrapper for print_new_trips_for_mode(). '''
    return new_trips_for_mode(comparison, [11, 12], 'Change in Drive-to-Transit {0}'.format(table), table)
def new_trips_wtt(comparison, table='Trips'):
    ''' Walk-to-transit wrapper for print_new_trips_for_mode(). '''
    return new_trips_for_mode(comparison, [9, 10], 'Change in Walk-to-Transit {0}'.format(table), table)
def new_trips_other(comparison, table='Trips'):
    ''' Walk/bike/taxi/school bus trips wrapper for print_new_trips_for_mode(). '''
    return new_trips_for_mode(comparison, [7, 8, 13, 14], 'Change in Non-Auto, Non-Transit {0}'.format(table), table)


def ptrips_by_class_change(comparison):
    ''' Print the change in transit person-trips by user class. '''
    print ' '
    print 'CHANGE IN LINKED TRANSIT PERSON-TRIPS BY USER CLASS'
    print '---------------------------------------------------'
    total_base_ptrips = sum(comparison.base.ptrips_by_class.itervalues())
    total_test_ptrips = sum(comparison.test.ptrips_by_class.itervalues())
    total_ptrips_diff = total_test_ptrips - total_base_ptrips
    total_pct_diff = total_ptrips_diff / total_base_ptrips
    for uclass in sorted(comparison.base.ptrips_by_class.keys()):
        base_ptrips = comparison.base.ptrips_by_class[uclass]
        test_ptrips = comparison.test.ptrips_by_class[uclass]
        ptrips_diff = test_ptrips - base_ptrips
        ptrips_pct_diff = ptrips_diff / base_ptrips
        print '{0:<25}{1:>+10,.0f} ({2:+.2%})'.format('User Class {0}'.format(uclass), ptrips_diff, ptrips_pct_diff)
    print '{0:<25}{1:>+10,.0f} ({2:+.2%})'.format('All User Classes', total_ptrips_diff, total_pct_diff)
    print ' '
    return None


def transit_stats_change(comparison, grouped=True):
    ''' Print the change in transit stats, by mode or grouped. '''
    def stat_txt(stat_name, mode=None, is_grouped=grouped):
        ''' Helper function to return formatted output text. '''
        if is_grouped:
            base_stat = sum(comparison.base.transit_stats[stat_name].itervalues())
            test_stat = sum(comparison.test.transit_stats[stat_name].itervalues())
        else:
            base_stat = comparison.base.transit_stats[stat_name][mode]
            test_stat = comparison.test.transit_stats[stat_name][mode]
        stat_diff = test_stat - base_stat
        stat_pct_diff = stat_diff / base_stat
        return '{0:+,.0f} ({1:+7.2%})'.format(stat_diff, stat_pct_diff)
    print ' '
    if grouped:
        print 'TRANSIT STATS CHANGE'
        print '--------------------'
        print ' {0:<20} | {1:<20} | {2:<20} '.format('Boardings', 'Pass. Miles', 'Pass. Hours')
        print '{0:-<22}|{0:-<22}|{0:-<22}'.format('')
        brd_txt = stat_txt('BOARDINGS')
        pmt_txt = stat_txt('PMT')
        pht_txt = stat_txt('PHT')
        print ' {0:>20} | {1:>20} | {2:>20} '.format(brd_txt, pmt_txt, pht_txt)
    else:
        print 'TRANSIT STATS CHANGE BY MODE'
        print '----------------------------'
        print ' {0:<20} | {1:<20} | {2:<20} | {3:<20} '.format('Mode', 'Boardings', 'Pass. Miles', 'Pass. Hours')
        print '{0:-<22}|{0:-<22}|{0:-<22}|{0:-<22}'.format('')
        for mode_code, mode_desc in sorted(abm.ABM.transit_modes.iteritems(), key=lambda x: x[1]):
            brd_txt = stat_txt('BOARDINGS', mode_code)
            pmt_txt = stat_txt('PMT', mode_code)
            pht_txt = stat_txt('PHT', mode_code)
            print ' {0:<20} | {1:>20} | {2:>20} | {3:>20} '.format(mode_desc, brd_txt, pmt_txt, pht_txt)
    print ' '
    return None


def vmt_by_speed_change(comparison):
    ''' Print the change in total daily VMT by 5-mph speed bin. '''
    print ' '
    print 'CHANGE IN DAILY VMT BY SPEED (MPH)'
    print '----------------------------------'
    total_base_vmt = sum(comparison.base.vmt_by_speed.itervalues())
    total_test_vmt = sum(comparison.test.vmt_by_speed.itervalues())
    total_vmt_diff = total_test_vmt - total_base_vmt
    total_pct_diff = total_vmt_diff / total_base_vmt
    for speed_bin in sorted(comparison.base.vmt_by_speed.keys()):
        base_vmt = comparison.base.vmt_by_speed[speed_bin]
        test_vmt = comparison.test.vmt_by_speed[speed_bin]
        vmt_diff = test_vmt - base_vmt
        vmt_pct_diff = vmt_diff / base_vmt
        if speed_bin <= 65:
            bin_label = '{0}-{1}'.format(speed_bin, speed_bin+5)
        else:
            bin_label = '70+'
        print '{0:<6}{1:>15,.2f} ({2:+.2%})'.format(bin_label, vmt_diff, vmt_pct_diff)
    print '{0:<6}{1:>15,.2f} ({2:+.2%})'.format('Total', total_vmt_diff, total_pct_diff)
    print ' '
    return None
