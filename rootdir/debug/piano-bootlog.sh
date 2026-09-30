#!/system/bin/sh
# Bring-up only: keep kernel and Android logs of the current boot on /metadata
# so they can be read from recovery after a failed boot.
dir=/metadata/piano-bootlog
mkdir -p $dir
rm -rf $dir/prev
mkdir -p $dir/prev
mv $dir/*.txt $dir/prev/ 2>/dev/null
n=0
while [ $n -lt 600 ]; do
    dmesg > $dir/dmesg.txt 2>&1
    logcat -b all -d > $dir/logcat.txt 2>&1
    getprop > $dir/props.txt 2>&1
    echo "$n $(cat /proc/uptime)" > $dir/tick.txt
    sync
    n=$((n + 1))
    sleep 2
done
