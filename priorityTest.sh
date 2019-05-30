

cd /home/userfs/z/zs673/PriorityAssignments/result
pwd

rm -rf *.txt

cd /home/userfs/z/zs673/PriorityAssignments
pwd
rm nohup.out

javac $(find ./src/* | grep .java)


nohup java -cp src/ evaluation.CompleteExperiments &

#LD_LIBRARY_PATH=src nohup java -cp /home/userfs/z/zs673/PriorityAssignments/bin evaluation.CompleteExperiments &
