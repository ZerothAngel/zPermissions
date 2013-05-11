#!/usr/bin/env python
from __future__ import print_function

import sys
try:
    import yaml
except ImportError:
    exit('Install PyYAML from <http://pyyaml.org> first')


__author__ = 'ZerothAngel'
__license__ = 'Public Domain'


def parse_perms(perms):
    result = {}
    for p in perms:
        if p.startswith('^'):
            result[p[1:]] = False
        else:
            result[p] = True
    return result


def parse_file(groups, stream):
    y = yaml.load(stream)

    g = y.get('groups', {})
    for group_name,data in g.items():
        perms = data.get('permissions', [])
        parents = data.get('groups', [])
        meta = data.get('meta', {})

        group_data = {}
        group_data['permissions'] = parse_perms(perms)
        group_data['parents'] = parents
        group_data['prefix'] = meta.get('prefix')
        group_data['suffix'] = meta.get('suffix')

        groups[group_name] = group_data


def dump_group(name, data, out):
    # Preamble
    print('# Group %s' % name, file=out)
    print('permissions group %s create' % name, file=out)
    # Permissions
    for perm,value in data['permissions'].items():
        print('permissions group %s set %s %s' %
              (name, perm, str(value).lower()), file=out)
        if perm.lower().startswith('group.'):
            print('WARNING: Group %s has a %s permission; '
                  'zPerms sets these automatically. Consider deleting it.' %
                  (name, perm))
    # Parent
    parents = data['parents']
    if parents:
        print('permissions group %s setparent %s' % (name, parents[0]),
              file=out)
        if len(parents) > 1:
            print('WARNING: Group %s has more than one parent. Using only one.' % name, file=sys.stderr)

    # Prefix/suffix
    prefix = data['prefix']
    if prefix:
        print('permissions group %s metadata set prefix %s' % (name, prefix),
              file=out)
    suffix = data['suffix']
    if suffix:
        print('permissions group %s metadata set suffix %s' % (name, suffix),
              file=out)


def make_dump(groups, out):
    # Gather root groups (parentless groups)
    roots = [k for k,v in groups.items() if not v['parents']]

    to_dump = roots
    while to_dump:
        group_name = to_dump.pop(0)
        dump_group(group_name, groups[group_name], out)

        # Queue up all children
        # Have to scan. Bleah.
        children = [k for k,v in groups.items() if group_name in v['parents']]

        to_dump.extend(children)


if __name__ == '__main__':
    groups = {}

    if len(sys.argv) < 2:
        exit('Usage: %s <output-file> [<groups.yml> ...]' % sys.argv[0])
    elif len(sys.argv) > 2:
        for fn in sys.argv[2:]:
            with open(fn, 'rt') as f:
                parse_file(groups, f)
    else:
        parse_file(groups, sys.stdin)

    with open(sys.argv[1], 'wt') as out:
        make_dump(groups, out)
