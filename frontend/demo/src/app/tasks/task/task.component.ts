import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TaskService } from '@umb-ag/curo-camunda';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-task',
  templateUrl: './task.component.html',
  styleUrls: ['./task.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskComponent implements OnInit {
  formKey$: Observable<string> | undefined;

  constructor(
    private activatedRoute: ActivatedRoute,
    private taskService: TaskService
  ) {}

  ngOnInit(): void {
    this.formKey$ = this.activatedRoute.params.pipe(
      map((params) => params.taskId),
      switchMap((taskId) => this.taskService.getForm(taskId)),
      map((form) => form.key)
    );
  }
}
