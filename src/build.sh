all_java='*.java'
for d in */ ; do
        echo "building $d$all_java"
        javac $d$all_java
done
