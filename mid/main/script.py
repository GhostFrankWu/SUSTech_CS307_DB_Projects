import re
import json

def load():
    data=[]
    with open('course_info.json', 'r', encoding='utf-8') as f:
        data = json.load(f)
    return data;

def change(x):
    r=""
    x=x.replace("或者","OR");
    x=x.replace("并且","AND");
    l=re.split(r'([\(\) ])',x)
    for i in l:
        if i!='OR' and i != 'AND' and i != ')' and i != '(' and i != ' ' and i != '':
            r+=("course_learned like ''% {} %''".format(i))
        else:
            r+=(i)
    return r


def insert_course(data):
    course=[]
    for i in data:
        course.append('''INSERT INTO public.courses (name, course_id, credit, course_hour, prerequisite) VALUES ('{}','{}',{},{},{});'''.format(i['courseName'],i['courseId'],i['courseCredit'],i['courseHour'],'null' if i['prerequisite']==None else "'"+str(change(str(i['prerequisite'])))+"'"))
    course=set(course)
    for i in course:
        print(i)

def insert_teacher(data):
    teacher=[]
    for i in data:
        teacher.append(i['teacher'])
    teacher=set(teacher)
    for i in teacher:
        print('''INSERT INTO public.teachers(name,placeholder) VALUES ('{}','{}');'''.format(1,1))####

def load_csv():
    col={'格兰芬多(Gryffindor)':5,
    '拉文克劳(Ravenclaw)':4,
    '赫奇帕奇(Hufflepuff)':3,
    '斯莱特林(Slytherin)':2,
   '阿兹卡班(Azkaban)':1
    }
dat=[]
with open('select_course.csv', 'r', encoding='utf-8') as f:
    for line in f.readlines():
        data=str(line).split(',')
        name,gender,college,id=data[0],data[1],data[2],data[3]
        s=""
        for i in range(4,len(data)):
            s+=" "+data[i]+" ,"
        s=s.replace("\n","")
        dat.append('''INSERT INTO public.students (name, gender, college_id, student_id, current_course) VALUES ('{}','{}',{},{},'{}');\n'''.format(name,gender,col[college],id,s))
    f.close()
with open('ins.sql', 'w+', encoding='utf-8') as f:
    f.writelines(dat)
    f.close()


def insert_class_and_timeTable(data):
cla=[]
tab=[]
ind=1;
for i in data:
    k=""
    for l in i['classList']:
        s=('''INSERT INTO public.time_table (weeklist, location, classtime, weekday,class_id) VALUES ('{}','{}','{}',{},{});'''.format(str(l['weekList']).replace("'","''"),l['location'],l['classTime'],l['weekday'],ind))
        tab.append(s)
        k+=s
    cla.append('''INSERT INTO public.classes(course_id, name, coursedept, time_hash, teacher_name) VALUES ('{}','{}','{}',{},'{}');'''.format(i['courseId'],i['className'],i['courseDept'],hash(k)%1000000007,i['teacher']))
    ind=ind+1

for i in data:
    k=""
    for l in i['classList']:
        s=('''INSERT INTO public.time_table (weeklist, location, classtime, weekday,class_id) VALUES ('{}','{}','{}',{},{});'''.format(str(l['weekList']).replace("'","''"),l['location'],l['classTime'],l['weekday'],ind))
        tab.append(s)
        k+=s
    cla.append('''UPDATE classes SET capacity = {} WHERE course_id like '%{}%' and name like '%{}%' and coursedept like '%{}%' and teacher_name like '%{}%';'''.format(i['totalCapacity'],i['courseId'],i['className'],i['courseDept'],i['teacher']))
    ind=ind+1

#c:a dict of teacher_name->teacher_id
for i in range(0,len(data)):
    r=str(data[i]['teacher']).replace("\t","").split(",")
    for j in r:
        t.append("insert into course_teach (class_id, teacher_id) values ({},{});".format(i+1,c[j]))
    