import os
from fnmatch import fnmatch
from xml.dom import minidom

test_root = '/usr/local/google/home/lpf/supportlib/master/out/host/gradle/frameworks/support'
test_pattern = "TEST*.xml"

times = {}

for path, subdirs, files in os.walk(test_root):
    for name in files:
        if fnmatch(name, test_pattern):
            xmldoc = minidom.parse(os.path.join(path, name))
            itemlist = xmldoc.getElementsByTagName('testcase')
            for s in itemlist:
                try:
                    classname = s.attributes['classname'].value.rsplit('.',1)[1]
                    times[classname] = 0
                except IndexError:
                    print(s.attributes['classname'].value)
            for s in itemlist:
                try:
                    time = float(s.attributes['time'].value)
                    classname = s.attributes['classname'].value.rsplit('.',1)[1]
                    times[classname] += time
                except IndexError:
                    print(s.attributes['classname'].value)
medium = []
large = []

for classname in times:
    if (times[classname] > 1):
        large.append(classname)
    elif (times[classname] > 0.2):
        medium.append(classname)

root = '/usr/local/google/home/lpf/supportlib/master/frameworks/support'

for path, subdirs, files in os.walk(root):
    for name in files:
        if name.split('.',1)[0] in medium:
            with open(os.path.join(path, name)) as f:
                newText=f.read().replace('@SmallTest', '@MediumTest')
                newText = newText.replace('androidx.test.filters.SmallTest', 'androidx.test.filters.MediumTest')
            with open(os.path.join(path, name), "w") as f:
                f.write(newText)

        if name.split('.',1)[0] in large:
            with open(os.path.join(path, name)) as f:
                newText=f.read().replace('@SmallTest', '@LargeTest')
                newText = newText.replace('androidx.test.filters.SmallTest', 'androidx.test.filters.LargeTest')
            with open(os.path.join(path, name), "w") as f:
                f.write(newText)
