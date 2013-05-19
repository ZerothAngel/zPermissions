#!/usr/bin/env python
from __future__ import print_function

import sys
import re
try:
    import yaml
except ImportError:
    exit('Install PyYAML from <http://pyyaml.org> first')


__author__ = 'ZerothAngel'
__license__ = 'Public Domain'


MULTI_PERM = re.compile(r"""(.*)\(((?:[^)]+\|)*(?:[^)]+))\)(.*)""")


def sorted_items(d):
    result = []
    result.extend(d.items())
    result.sort(key=lambda x: x[0].lower())
    return result


def expand_perm(perm):
    m = MULTI_PERM.match(perm)
    if m:
        return [m.group(1) + p + m.group(3) for p in m.group(2).split('|')]
    else:
        return [perm]


def parse_perms(world, perms):
    if world:
        world = world + ':'
    else:
        world = ''

    result = {}
    for p in perms:
        if p.startswith('-'):
            for perm in expand_perm(p[1:]):
                result[world + perm] = False
        else:
            for perm in expand_perm(p):
                result[world + perm] = True
    return result


def parse_worlds(worlds):
    if not worlds:
        return {}
    result = {}
    for world_name,data in worlds.items():
        perms = data.get('permissions', [])

        result.update(parse_perms(world_name, perms))
    return result


def parse_file(players, groups, stream):
    y = yaml.load(stream)

    g = y.get('groups', {})
    for group_name,data in g.items():
        perms = data.get('permissions', [])
        worlds = data.get('worlds', {})
        parents = data.get('inheritance', [])

        group_data = {}
        group_data['permissions'] = parse_perms(None, perms)
        group_data['permissions'].update(parse_worlds(worlds))
        group_data['parents'] = parents
        group_data['prefix'] = data.get('prefix')
        group_data['suffix'] = data.get('suffix')

        groups[group_name] = group_data

    u = y.get('users', {})
    for user_name,data in u.items():
        perms = data.get('permissions', [])
        worlds = data.get('worlds', {})
        memberships = data.get('group', [])

        if memberships:
            for group_name in memberships:
                group_data = groups.get(group_name)
                if group_data is None:
                    print('WARNING: %s is a member of %s, but the group does not exist' % (user_name, group_name), file=sys.stderr)
                    continue
                members = groups[group_name].get('members', [])
                members.append(user_name)
                groups[group_name]['members'] = members

        player_data = {}
        player_data['permissions'] = parse_perms(None, perms)
        player_data['permissions'].update(parse_worlds(worlds))
        player_data['prefix'] = data.get('prefix')
        player_data['suffix'] = data.get('suffix')

        players[user_name] = player_data


def dump_permissions(name, is_group, permissions, out):
    entity_type = is_group and 'group' or 'player'
    for perm,value in sorted_items(permissions):
        print('permissions %s %s set %s %s' %
              (entity_type, name, perm, str(value).lower()), file=out)
        if is_group and perm.lower().startswith('group.'):
            print('WARNING: Group %s has a %s permission; '
                  'zPerms sets these automatically. Consider deleting it.' %
                  (name, perm))


def dump_prefix_suffix(name, is_group, prefix, suffix, out):
    entity_type = is_group and 'group' or 'player'
    if prefix:
        print('permissions %s %s metadata set prefix %s' % (entity_type, name, prefix),
              file=out)
    if suffix:
        print('permissions %s %s metadata set suffix %s' % (entity_type, name, suffix),
              file=out)

def dump_player(name, data, out):
    perms = data['permissions']
    prefix = data['prefix']
    suffix = data['suffix']

    if not (perms or prefix or suffix):
        return # Nothing to do

    # Preamble
    print('# Player %s' % name, file=out)
    # Permissions
    dump_permissions(name, False, perms, out)

    # Prefix/suffix
    dump_prefix_suffix(name, False, prefix, suffix, out)


def dump_group(name, data, out):
    # Preamble
    print('# Group %s' % name, file=out)
    print('permissions group %s create' % name, file=out)
    # Permissions
    dump_permissions(name, True, data['permissions'], out)
    # Parent
    parents = data['parents']
    if parents:
        print('permissions group %s setparent %s' % (name, parents[0]),
              file=out)
        if len(parents) > 1:
            print('WARNING: Group %s has more than one parent. Using only one.' % name, file=sys.stderr)

    # Prefix/suffix
    dump_prefix_suffix(name, True, data['prefix'], data['suffix'], out)

    # Members
    members = data.get('members', [])
    for member in members:
        print('permissions group %s add %s' % (name, member), file=out)

 
def make_dump(players, groups, out):
    # Dump players
    for player_name,data in players.items():
        dump_player(player_name, data, out)

    # Gather root groups (parentless groups)
    roots = [k for k,v in groups.items() if not v['parents']]

    to_dump = roots
    while to_dump:
        group_name = to_dump.pop(0)
        dump_group(group_name, groups[group_name], out)

        # Queue up all children
        # Have to scan. Bleah.
        children = [k for k,v in groups.items() if group_name in v['parents']]
        children.sort()

        to_dump.extend(children)


if __name__ == '__main__':
    players = {}
    groups = {}

    if len(sys.argv) < 2:
        exit('Usage: %s <output-file> [<permissions.yml> ...]' % sys.argv[0])
    elif len(sys.argv) > 2:
        for fn in sys.argv[2:]:
            with open(fn, 'rt') as f:
                parse_file(players, groups, f)
    else:
        parse_file(players, groups, sys.stdin)

    with open(sys.argv[1], 'wt') as out:
        make_dump(players, groups, out)
