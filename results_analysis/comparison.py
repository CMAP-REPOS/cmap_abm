#!/usr/bin/env python
'''
    comparison.py
    Author: npeterson
    Revised: 3/11/16
    ---------------------------------------------------------------------------
    A class for comparing the contents of two ABM object databases.

'''
import copy
import abm


class Comparison(object):
    ''' A class for comparing two ABM objects. '''

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


    def export_persontrips_csv(self, csv_path, geography='zone', trip_end='origin'):
        ''' Export a CSV file containing the mean base & test user classes for
            person-trips originating/ending in each zone/subzone. '''
        if trip_end not in ('origin', 'destination'):
            print 'CSV not exported: trip_end must be "origin" or "destination"!'
            return None
        elif geography == 'zone':
            group_field = 'Trips.zn_{0}'.format(trip_end[0])
        elif geography == 'subzone':
            group_field = 'Trips.sz_{0}'.format(trip_end[0])
        else:
            print 'CSV not exported: geography must be "zone" or "subzone"!'
            return None

        max_id = {
            'zone': 1944,
            'subzone': 16819
        }

        grouped_base_ptrips_by_class = self.base._get_ptrips_by_class(group_field)
        grouped_test_ptrips_by_class = self.test._get_ptrips_by_class(group_field)

        with open(csv_path, 'wb') as w:
            w.write('{0}_{1},ptrips_base,mean_uclass_base,ptrips_test,mean_uclass_test,ptrips_diff,mean_uclass_diff\n'.format(geography, trip_end[0]))
            for geog_id in xrange(1, max_id[geography]+1):
                where_filter = '{0} = {1}'.format(group_field, geog_id)

                if geog_id in grouped_base_ptrips_by_class:
                    base_ptrips_by_class = grouped_base_ptrips_by_class[geog_id]
                    base_ptrips_total = sum(base_ptrips_by_class.itervalues())
                    if base_ptrips_total > 0:
                        mean_uclass_base = sum((uc * n for uc, n in base_ptrips_by_class.iteritems())) / base_ptrips_total
                    else:
                        mean_uclass_base = 0
                else:
                    mean_uclass_base = 0

                if geog_id in grouped_test_ptrips_by_class:
                    test_ptrips_by_class = grouped_test_ptrips_by_class[geog_id]
                    test_ptrips_total = sum(test_ptrips_by_class.itervalues())
                    if test_ptrips_total > 0:
                        mean_uclass_test = sum((uc * n for uc, n in test_ptrips_by_class.iteritems())) / test_ptrips_total
                    else:
                        mean_uclass_test = 0
                else:
                    mean_uclass_test = 0

                ptrips_total_diff = test_ptrips_total - base_ptrips_total
                mean_uclass_diff = mean_uclass_test - mean_uclass_base

                row_template = '{0},{1:.0f},{2:.4f},{3:.0f},{4:.4f},{5:.0f},{6:.4f}\n'
                w.write(row_template.format(geog_id, base_ptrips_total, mean_uclass_base, test_ptrips_total, mean_uclass_test, ptrips_total_diff, mean_uclass_diff))

        print 'Person-trips and mean user class by {0} {1} have been exported to {2}.\n'.format(trip_end, geography, csv_path)
        return None


    def _get_trip_mode_diffs(self, trips_or_ptrips='TRIPS'):
        ''' Calculate the change in trips, by mode, for each person. '''
        if trips_or_ptrips == 'TRIPS':
            sql_template = 'SELECT hh_id||"-"||pers_num AS hh_pers, COUNT(*) FROM Trips WHERE mode in ({0}) GROUP BY hh_pers'
            unique_pers_query = 'SELECT DISTINCT hh_id||"-"||pers_num AS hh_pers FROM Trips'
        elif trips_or_ptrips == 'PERSON-TRIPS':
            sql_template = 'SELECT pers_id, COUNT(*) FROM PersonTrips WHERE mode in ({0}) GROUP BY pers_id'
            unique_pers_query = 'SELECT DISTINCT pers_id FROM PersonTrips'
        else:
            raise ValueError('Argument trips_or_ptrips must be "TRIPS" or "PERSON-TRIPS".')

        mode_groups = {
            'auto': '1, 2, 3, 4, 5, 6',
            'dtt': '11, 12',
            'wtt': '9, 10',
            'other': '7, 8, 13, 14'
        }

        # Find all unique person-IDs from both scenarios
        unique_pers_base = set(r[0] for r in self.base.query(unique_pers_query))
        unique_pers_test = set(r[0] for r in self.test.query(unique_pers_query))
        unique_pers = unique_pers_base | unique_pers_test  # Union of the two

        # Create base/test/diff trips dicts, initialized with 0's
        trips_dict_template = {group_key: 0 for group_key in mode_groups.keys()}
        trips_dict_base = {pers_id: trips_dict_template.copy() for pers_id in unique_pers}
        trips_dict_test = copy.deepcopy(trips_dict_base)
        trips_dict_diff = copy.deepcopy(trips_dict_base)

        # Populate dicts
        for group_key, mode_list in mode_groups.iteritems():
            mode_sql = sql_template.format(mode_list)
            for pers_id, count in self.base.query(mode_sql):
                trips_dict_base[pers_id][group_key] = self.base._unsample(count)
            for pers_id, count in self.test.query(mode_sql):
                trips_dict_test[pers_id][group_key] = self.test._unsample(count)
            for pers_id in trips_dict_diff.iterkeys():
                trips_dict_diff[pers_id][group_key] = trips_dict_test[pers_id][group_key] - trips_dict_base[pers_id][group_key]

        return trips_dict_diff

    # Wrapper methods for _get_trip_mode_diffs():
    def _get_ptrip_mode_diffs(self):
        ''' Calculate the change in person-trips, by mode, for each person. '''
        return self._get_trip_mode_diffs('PERSON-TRIPS')


    def open_dbs(self):
        ''' Open base & test ABM database connections. '''
        self.base.open_db()
        self.test.open_db()
        return None


    def print_auto_trips_affected(self, trips_or_ptrips='TRIPS'):
        ''' Print the estimated number of auto trips diverted or eliminated. '''
        if trips_or_ptrips == 'TRIPS':
            mode_diffs = self._get_trip_mode_diffs()
        elif trips_or_ptrips == 'PERSON-TRIPS':
            mode_diffs = self._get_ptrip_mode_diffs()
        else:
            raise ValueError('Argument trips_or_ptrips must be "TRIPS" or "PERSON-TRIPS".')

        auto_trips_diverted = {}
        auto_trips_eliminated = {}

        # Estimate trips diverted/eliminated for each person individually.
        for pers, trip_diff_dict in mode_diffs.iteritems():
            diff_auto = trip_diff_dict['auto']
            diff_dtt = trip_diff_dict['dtt']
            diff_wtt_oth = trip_diff_dict['wtt'] + trip_diff_dict['other']

            auto_trips_diverted[pers] = 0
            auto_trips_eliminated[pers] = 0

            # Account for all lost auto-only trips
            if diff_auto < 0:
                while diff_auto < 0:

                    # First assume lost trips were diverted (drive-to-transit)
                    if diff_dtt > 0:
                        auto_trips_diverted[pers] += 1
                        diff_dtt -= 1
                        diff_auto += 1

                    # Then assume lost trips were eliminated (walk-to-transit/other)
                    elif diff_wtt_oth > 0:
                        auto_trips_eliminated[pers] += 1
                        diff_wtt_oth -= 1
                        diff_auto += 1

                    # Finally, add the remainder (auto trips completely eliminated,
                    # not replaced/shortened with transit) to trips eliminated
                    else:
                        auto_trips_eliminated[pers] += abs(diff_auto)
                        diff_auto = 0

            # Account for all gained auto-only trips
            elif diff_auto > 0:
                while diff_auto > 0:

                    # First assume gained trips were lengthened from drive-to-transit
                    if diff_dtt < 0:
                        auto_trips_diverted[pers] -= 1
                        diff_dtt += 1
                        diff_auto -= 1

                    # Then assume gained trips replaced walk-to-transit/other
                    elif diff_wtt_oth < 0:
                        auto_trips_eliminated[pers] -= 1
                        diff_wtt_oth += 1
                        diff_auto -= 1

                    # Finally, subtract the remainder (totally new auto trips)
                    # from trips eliminated
                    else:
                        auto_trips_eliminated[pers] -= abs(diff_auto)
                        diff_auto = 0

            ## Account for auto portion of any new or eliminated drive-to-transit trips.
            ## (Is it fair to count these as auto trips added/eliminated?)
            #if diff_dtt != 0:
            #    auto_trips_eliminated[pers] -= diff_dtt
            #    diff_dtt = 0

        auto_trips_diverted = sum(auto_trips_diverted.itervalues())
        auto_trips_eliminated = sum(auto_trips_eliminated.itervalues())

        print ' '
        print '{0:<30}{1:>8,.0f}'.format('AUTO {0} DIVERTED:'.format(trips_or_ptrips), auto_trips_diverted)
        print '{0:<30}{1:>8,.0f}'.format('AUTO {0} ELIMINATED:'.format(trips_or_ptrips), auto_trips_eliminated)
        print ' '

        return None

    # Wrapper methods for print_auto_trips_affected():
    def print_auto_ptrips_affected(self):
        ''' Print the estimated number of auto person-trips diverted or eliminated. '''
        return self.print_auto_trips_affected('PERSON-TRIPS')


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
        ''' Identify the increase (or decrease) in trips/tours for a given set of modes. '''
        sql_where = ' or '.join(('mode={0}'.format(mode) for mode in mode_list))
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
