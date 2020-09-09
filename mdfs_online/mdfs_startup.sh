cd "$(dirname "$0")"

JARFILENAME="MDFS_online.jar"
#rm logs/*
# Carry out specific functions when asked to by the system
case "$1" in
  start)
    rm logs/*
    echo "Starting MDFS_online Service"
    java -jar $JARFILENAME > /dev/null &
    ;;
  stop)
    echo "Stopping MDFS_online Service"
    kill $(ps -ef | grep MDFS_online | awk '{print $2}')
    ;;
  *)
    echo "Usage: mdfs_startup {start|stop}"
    exit 1
    ;;
esac

exit 0


