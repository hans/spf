# Data folds
#type=data.clevr id=clevr questionsFile=data/CLEVR_train_questions.json scenesFile=data/CLEVR_train_scenes.json
#type=data.clevr.situated id=clevrCurriculum1 questionsFile=../../data/curriculum.1_basenouns.train.json scenesFile=../../data/curriculum_scenes.train.json
#type=data.clevr.situated id=clevrCurriculum2 questionsFile=../../data/curriculum.2_1adj.train.json scenesFile=../../data/curriculum_scenes.train.json
#type=data.clevr.situated id=clevrCurriculum3 questionsFile=../../data/curriculum.3_propquery.train.json scenesFile=../../data/curriculum_scenes.train.json
#type=data.clevr.situated id=clevrSimpleSimpleRef questionsFile=../../data/simplesimpleref_questions.json scenesFile=../../data/simpleref_scenes.json shuffle=true
type=data.clevr.situated id=shapeColorTrain questionsFile=../../data/shapecolor_train_questions.json scenesFile=../../data/shapecolor_train_scenes.json shuffle=true
type=data.clevr.situated id=shapeColorVal questionsFile=../../data/shapecolor_val_questions.json scenesFile=../../data/shapecolor_val_scenes.json shuffle=true
