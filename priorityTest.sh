cd /home/userfs/z/zs673/PriorityAssignments

javac $(find ./src/* | grep .java)


nohup java -cp src/ evaluation.CompleteExperiments &> nohup1and20.out&