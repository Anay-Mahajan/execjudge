import requests

QID=1
api=f"http://localhost:8080/api/task/addTestCase"
for i in range(1,17):
    file_input=f"tests/{i}.in"
    file_output=f"tests/{i}.out"
    input=""
    output=""
    with open(file_input,"r") as f:
        input=f.read()
    with open(file_output,"r") as f:
        output=f.read()
    payload={
        "input":input,
        "expectedOutput":output,
        "qid":QID
    }
    r = requests.post(api,
                json=payload,
                timeout=3)
    if r.status_code != 200:
        raise Exception("Submit failed")

    print(r.json())

