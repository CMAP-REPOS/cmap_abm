from results import *

QUANTILES = 5
REBUILD_DBS = False

# Initialize ABM objects
b = ABM(r'X:\CMAQ_ABM_Models\cmaq_base_20141204', 0.20, REBUILD_DBS)

t_nd1 = ABM(r'X:\CMAQ_ABM_Models\cmaq_node_min_20141222', 0.20, REBUILD_DBS)
t_nd2 = ABM(r'X:\CMAQ_ABM_Models\cmaq_node_mod_20150109', 0.20, REBUILD_DBS)
t_nd3 = ABM(r'X:\CMAQ_ABM_Models\cmaq_node_max_20141215', 0.20, REBUILD_DBS)

t_ln1 = ABM(r'X:\CMAQ_ABM_Models\cmaq_line_min_20150105', 0.20, REBUILD_DBS)
t_ln2 = ABM(r'X:\CMAQ_ABM_Models\cmaq_line_mod_20150112', 0.20, REBUILD_DBS)
t_ln3 = ABM(r'X:\CMAQ_ABM_Models\cmaq_line_max_20141208', 0.20, REBUILD_DBS)


# Get base boardings and quantiles
boardings_b = {
    'LINE': b._get_boardings('LINE'),
    'NODE': b._get_boardings('NODE')
}

quantiles_b = {
    'LINE': b._get_boarding_quantiles(boardings_b['LINE'], QUANTILES),
    'NODE': b._get_boarding_quantiles(boardings_b['NODE'], QUANTILES)
}


# Print base bus boardings
print '\n', b

for node_or_line in sorted(quantiles_b.keys()):
    print node_or_line
    qnt_b = quantiles_b[node_or_line]
    brd_b = boardings_b[node_or_line]

    # Print mean base boardings by quantile
    for i in xrange(QUANTILES):
        n = sum((1 for k in qnt_b.keys() if qnt_b[k] == i))
        mean_base_brd = sum((brd_b[k] for k in qnt_b.keys() if qnt_b[k] == i)) / n if n else 0.
        print 'Q{0} ({1}): {2}'.format(i+1, n, mean_base_brd)

    # Print mean base boardings for all nodes/lines
    n = sum((1 for k in qnt_b.keys()))
    mean_base_brd = sum((brd_b[k] for k in qnt_b.keys())) / n if n else 0.  # Mean of all boardings
    print 'ALL ({0}): {1}\n'.format(n, mean_base_brd)


# Compare test scenario boardings with base, by quantile
def compare_boardings(t, node_or_line, mode=''):
    print '\n', t, mode

    if mode:
        qnt_b = quantiles_b[node_or_line][mode]
        brd_b = boardings_b[node_or_line][mode]
        brd_t = t._get_boardings(node_or_line, split_rail=True)[mode]
    else:
        qnt_b = quantiles_b[node_or_line]
        brd_b = boardings_b[node_or_line]
        brd_t = t._get_boardings(node_or_line, split_rail=False)

    brd_diff = {k: brd_t[k] - brd_b[k] for k in brd_b.keys()}

    # Print mean additional boardings by quantile
    for i in xrange(QUANTILES):
        n = sum((1 for k in qnt_b.keys() if qnt_b[k] == i))
        mean_new_brd = sum((brd_diff[k] for k in qnt_b.keys() if qnt_b[k] == i)) / n if n else 0.
        print 'Q{0} ({1}): {2}'.format(i+1, n, mean_new_brd)

    # Print mean additional boardings for all nodes/lines
    n = sum((1 for k in qnt_b.keys()))
    mean_new_brd = sum((brd_diff[k] for k in qnt_b.keys())) / n if n else 0.  # Mean of all boardings
    print 'ALL ({0}): {1}\n'.format(n, mean_new_brd)

    return brd_t

boardings_t_ln1 = compare_boardings(t_ln1, 'LINE')
boardings_t_ln2 = compare_boardings(t_ln2, 'LINE')
boardings_t_ln3 = compare_boardings(t_ln3, 'LINE')

boardings_t_nd1 = compare_boardings(t_nd1, 'NODE')
boardings_t_nd2 = compare_boardings(t_nd2, 'NODE')
boardings_t_nd3 = compare_boardings(t_nd3, 'NODE')
