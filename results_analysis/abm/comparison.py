#!/usr/bin/env python
'''
    comparison.py
    Author: npeterson
    Revised: 3/15/16
    ---------------------------------------------------------------------------
    A class for comparing the contents of two ABM object databases.

'''
import copy
import abm


class Comparison(object):
    ''' A class for comparing two ABM objects. Initialized with a base and a
        test ABM object. All comparisons are relative to the base. '''

    # --- Init ---
    def __init__(self, base_abm, test_abm):
        self.base = base_abm
        self.test = test_abm
        return None


    def __str__(self):
        return '[Comparison: BASE {0}; TEST {1}]'.format(self.base, self.test)


    # --- Instance methods ---
    def close_dbs(self):
        ''' Close base & test ABM database connections. '''
        self.base.close_db()
        self.test.close_db()
        return None


    def open_dbs(self):
        ''' Open base & test ABM database connections. '''
        self.base.open_db()
        self.test.open_db()
        return None


    def print_mode_share_change(self, grouped=True):
        ''' Print the change in mode share, grouped into broader categories
            (default), or ungrouped. '''
        mode_share_diff = {}
        for mode in sorted(abm.ABM.modes.keys()):
            mode_share_diff[mode] = self.test.mode_share[mode] - self.base.mode_share[mode]
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
                print '{0:<25}{1:>+10.2%}'.format(self.base._get_mode_str(mode), mode_share_diff[mode])
        print ' '
        return None


    def print_new_for_mode(self, mode_list, mode_description, table='Trips'):
        ''' Identify the increase (or decrease) in trips/tours for a given set
            of modes. '''
        sql_where = ' OR '.join(('mode={0}'.format(mode) for mode in mode_list))
        base_trips = self.base._unsample(self.base._count_rows(table, sql_where))
        test_trips = self.test._unsample(self.test._count_rows(table, sql_where))
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

    # Wrapper methods for print_new_for_mode():
    def print_new_all(self, table='Trips'):
        ''' All-trips wrapper for print_new_for_mode(). '''
        return self.print_new_for_mode(xrange(1, 15), 'Change in Total {0}'.format(table), table)
    def print_new_auto(self, table='Trips'):
        ''' Auto-trips wrapper for print_new_for_mode(). '''
        return self.print_new_for_mode(xrange(1, 7), 'Change in Auto {0}'.format(table), table)
    def print_new_dtt(self, table='Trips'):
        ''' Drive-to-transit wrapper for print_new_for_mode(). '''
        return self.print_new_for_mode([11, 12], 'Change in Drive-to-Transit {0}'.format(table), table)
    def print_new_wtt(self, table='Trips'):
        ''' Walk-to-transit wrapper for print_new_for_mode(). '''
        return self.print_new_for_mode([9, 10], 'Change in Walk-to-Transit {0}'.format(table), table)
    def print_new_other(self, table='Trips'):
        ''' Walk/bike/taxi/school bus trips wrapper for print_new_for_mode(). '''
        return self.print_new_for_mode([7, 8, 13, 14], 'Change in Non-Auto, Non-Transit {0}'.format(table), table)


    def print_ptrips_by_class_change(self):
        ''' Print the change in transit person-trips by user class. '''
        print ' '
        print 'CHANGE IN LINKED TRANSIT PERSON-TRIPS BY USER CLASS'
        print '---------------------------------------------------'
        total_base_ptrips = sum(self.base.ptrips_by_class.itervalues())
        total_test_ptrips = sum(self.test.ptrips_by_class.itervalues())
        total_ptrips_diff = total_test_ptrips - total_base_ptrips
        total_pct_diff = total_ptrips_diff / total_base_ptrips
        for uclass in sorted(self.base.ptrips_by_class.keys()):
            base_ptrips = self.base.ptrips_by_class[uclass]
            test_ptrips = self.test.ptrips_by_class[uclass]
            ptrips_diff = test_ptrips - base_ptrips
            ptrips_pct_diff = ptrips_diff / base_ptrips
            print '{0:<25}{1:>+10,.0f} ({2:+.2%})'.format('User Class {0}'.format(uclass), ptrips_diff, ptrips_pct_diff)
        print '{0:<25}{1:>+10,.0f} ({2:+.2%})'.format('All User Classes', total_ptrips_diff, total_pct_diff)
        print ' '
        return None


    def print_transit_stats_change(self, grouped=True):
        ''' Print the change in transit stats, by mode or grouped. '''
        def stat_txt(stat_name, mode=None, is_grouped=grouped):
            ''' Helper function to return formatted output text. '''
            if is_grouped:
                base_stat = sum(self.base.transit_stats[stat_name].itervalues())
                test_stat = sum(self.test.transit_stats[stat_name].itervalues())
            else:
                base_stat = self.base.transit_stats[stat_name][mode]
                test_stat = self.test.transit_stats[stat_name][mode]
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


    def print_vmt_by_speed_change(self):
        ''' Print the change in total daily VMT by 5-mph speed bin. '''
        print ' '
        print 'CHANGE IN DAILY VMT BY SPEED (MPH)'
        print '----------------------------------'
        total_base_vmt = sum(self.base.vmt_by_speed.itervalues())
        total_test_vmt = sum(self.test.vmt_by_speed.itervalues())
        total_vmt_diff = total_test_vmt - total_base_vmt
        total_pct_diff = total_vmt_diff / total_base_vmt
        for speed_bin in sorted(self.base.vmt_by_speed.keys()):
            base_vmt = self.base.vmt_by_speed[speed_bin]
            test_vmt = self.test.vmt_by_speed[speed_bin]
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
